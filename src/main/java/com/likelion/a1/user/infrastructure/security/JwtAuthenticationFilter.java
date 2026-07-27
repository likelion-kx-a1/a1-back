package com.likelion.a1.user.infrastructure.security;

import com.likelion.a1.user.domain.model.AuthSession;
import com.likelion.a1.user.domain.repository.AuthSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  /**
   * 브라우저 네이티브 EventSource는 Authorization 헤더를 보낼 수 없어, SSE 구독 엔드포인트에 한해서만
   * access token을 쿼리 파라미터로도 받는다(docs_h/보안_취약점_점검.md #1). 쿼리 파라미터 토큰은 접근
   * 로그/Referer로 노출될 수 있으므로 이 경로 밖으로는 절대 넓히지 않는다 — 다른 모든 엔드포인트는
   * 여전히 Authorization 헤더만 신뢰한다.
   */
  private static final String SSE_SUBSCRIBE_PATH = "/api/v1/sse/subscribe";

  private final JwtTokenProvider jwtTokenProvider;
  private final AuthSessionRepository authSessionRepository;

  public JwtAuthenticationFilter(
      JwtTokenProvider jwtTokenProvider, AuthSessionRepository authSessionRepository) {
    this.jwtTokenProvider = jwtTokenProvider;
    this.authSessionRepository = authSessionRepository;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String token = resolveToken(request);

    if (token != null) {
      try {
        JwtPrincipal principal = jwtTokenProvider.parse(token);

        if (isSessionActive(principal.sessionId())) {
          UsernamePasswordAuthenticationToken authentication =
              new UsernamePasswordAuthenticationToken(
                  principal,
                  null,
                  List.of(
                      new SimpleGrantedAuthority("ROLE_" + principal.role()),
                      new SimpleGrantedAuthority(principal.role())));

          SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
          SecurityContextHolder.clearContext();
        }
      } catch (Exception exception) {
        SecurityContextHolder.clearContext();
      }
    }

    filterChain.doFilter(request, response);
  }

  /**
   * access token의 서명/만료만으로는 로그아웃·관리자 계정 정지/삭제를 즉시 반영할 수 없어(토큰이
   * 자체 만료 시각까지 계속 유효해버림 — docs_h/보안_취약점_점검.md #4), 토큰에 담긴 sessionId로 매
   * 요청마다 세션이 아직 살아있는지 확인한다. 로그아웃/관리자 조치가 세션을 revoke하면 그 즉시(다음
   * 요청부터) 이 access token도 함께 무효화된다.
   */
  private boolean isSessionActive(String sessionId) {
    return authSessionRepository
        .findBySessionId(sessionId)
        .map(AuthSession::isActive)
        .orElse(false);
  }

  private String resolveToken(HttpServletRequest request) {
    String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
      return authorizationHeader.substring(7);
    }

    if (SSE_SUBSCRIBE_PATH.equals(request.getRequestURI())) {
      String queryToken = request.getParameter("token");
      if (StringUtils.hasText(queryToken)) {
        return queryToken;
      }
    }

    return null;
  }
}
