package com.likelion.a1.sse.presentation.controller;

import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import com.likelion.a1.sse.infrastructure.SseRepository;
import com.likelion.a1.sse.presentation.dto.SseDtos.ConnectPayload;
import com.likelion.a1.user.infrastructure.security.JwtPrincipal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 3초 폴링을 대체하는 전역 SSE 알림 구독 엔드포인트. 인증된 토큰 주체의 채널만 구독할 수 있다 —
 * {@code userId}는 더 이상 쿼리 파라미터로 받지 않고, 토큰(`JwtAuthenticationFilter`가 이 경로에 한해
 * 쿼리 파라미터 `token`도 허용)에서 파싱된 {@link JwtPrincipal}로부터만 가져온다.
 */
@RestController
@RequestMapping("/api/v1/sse")
public class SseController {
  private final SseRepository sseRepository;

  public SseController(SseRepository sseRepository) {
    this.sseRepository = sseRepository;
  }

  @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public ResponseEntity<SseEmitter> subscribe(@AuthenticationPrincipal JwtPrincipal principal) {
    Long userId = requireUserId(principal);

    SseEmitter emitter = sseRepository.register(userId);
    sseRepository.send(userId, "connect", ConnectPayload.connected());

    return ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL, "no-cache")
        .header("X-Accel-Buffering", "no")
        .body(emitter);
  }

  private Long requireUserId(JwtPrincipal principal) {
    if (principal == null || principal.userId() == null) {
      throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN);
    }
    return principal.userId();
  }
}
