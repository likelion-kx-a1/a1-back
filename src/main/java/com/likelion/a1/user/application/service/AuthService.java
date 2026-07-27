package com.likelion.a1.user.application.service;

import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import com.likelion.a1.user.domain.model.AuthSession;
import com.likelion.a1.user.domain.model.User;
import com.likelion.a1.user.domain.repository.AuthSessionRepository;
import com.likelion.a1.user.domain.repository.UserRepository;
import com.likelion.a1.user.infrastructure.ratelimit.RedisRateLimiter;
import com.likelion.a1.user.infrastructure.security.JwtTokenProvider;
import com.likelion.a1.user.infrastructure.security.TokenHashService;
import com.likelion.a1.user.presentation.dto.AuthDtos.*;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
    아이디 중복 확인
    회원가입
    로그인
    Access Token 재발급
    로그아웃
 */
@Service
public class AuthService {
  private static final String TOKEN_TYPE = "Bearer";

  /**
   * 로그인 브루트포스 방어 정책(docs_h/보안_취약점_점검.md #5). 계정 잠금(loginId 기준)은 실제 공격
   * 대상 계정을 보호하고, IP 기준 제한은 한 IP가 여러 계정을 동시에 대입 공격하거나 잠금을 이용해
   * 특정 사용자를 골라 로그인을 방해하는 시나리오(계정 잠금의 잘 알려진 약점)의 파급력을 줄인다.
   */
  private static final long LOGIN_MAX_FAILURES = 5;
  private static final Duration LOGIN_FAILURE_WINDOW = Duration.ofMinutes(15);
  private static final Duration LOGIN_LOCKOUT_DURATION = Duration.ofMinutes(15);
  private static final long LOGIN_IP_MAX_ATTEMPTS = 30;
  private static final Duration LOGIN_IP_WINDOW = Duration.ofMinutes(10);

  private final UserRepository userRepository;
  private final AuthSessionRepository authSessionRepository;
  private final EmailVerificationService emailVerificationService;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final TokenHashService tokenHashService;
  private final RedisRateLimiter rateLimiter;

  public AuthService(
      UserRepository userRepository,
      AuthSessionRepository authSessionRepository,
      EmailVerificationService emailVerificationService,
      PasswordEncoder passwordEncoder,
      JwtTokenProvider jwtTokenProvider,
      TokenHashService tokenHashService,
      RedisRateLimiter rateLimiter) {
    this.userRepository = userRepository;
    this.authSessionRepository = authSessionRepository;
    this.emailVerificationService = emailVerificationService;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenProvider = jwtTokenProvider;
    this.tokenHashService = tokenHashService;
    this.rateLimiter = rateLimiter;
  }

  @Transactional(readOnly = true)
  public LoginIdCheckResponse checkLoginId(String loginId) {
    return new LoginIdCheckResponse(!userRepository.existsByLoginId(loginId));
  }

  @Transactional
  public SignupResponse signup(SignupRequest request) {
    if (userRepository.existsByLoginId(request.loginId())) {
      throw new BusinessException(ErrorCode.USER_LOGIN_ID_DUPLICATE);
    }

    if (userRepository.existsByEmail(request.email())) {
      throw new BusinessException(ErrorCode.USER_EMAIL_DUPLICATE);
    }

    emailVerificationService.validateVerified(request.email(), "SIGNUP");

    User user =
        User.signup(
            request.loginId(),
            request.email(),
            passwordEncoder.encode(request.password()),
            request.name(),
            request.birthDate(),
            request.phoneNumber());

    User saved = userRepository.save(user);

    emailVerificationService.markLatestUsed(request.email(), "SIGNUP");

    return new SignupResponse(saved.getId(), saved.getApprovalStatus(), saved.getAccountStatus());
  }

  @Transactional
  public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
    if (ipAddress != null
        && !rateLimiter.tryConsume(loginIpKey(ipAddress), LOGIN_IP_MAX_ATTEMPTS, LOGIN_IP_WINDOW)) {
      throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
    }

    User user =
        userRepository
            .findByLoginId(request.loginId())
            .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_ID_NOT_FOUND));

    if (rateLimiter.isLocked(loginLockKey(user.getLoginId()))) {
      throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
    }

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      recordLoginFailure(user.getLoginId());
      throw new BusinessException(ErrorCode.PASSWORD_NOT_MATCH);
    }

    rateLimiter.reset(loginFailureKey(user.getLoginId()));

    validateLoginAllowed(user);

    String sessionId = UUID.randomUUID().toString();
    String refreshToken = tokenHashService.generateRefreshToken();
    String refreshTokenHash = tokenHashService.sha256(refreshToken);

    AuthSession session =
        AuthSession.create(
            user.getId(),
            sessionId,
            refreshTokenHash,
            ipAddress,
            userAgent,
            OffsetDateTime.now().plusDays(14));

    authSessionRepository.save(session);
    user.recordLogin();

    String accessToken = jwtTokenProvider.createAccessToken(user, sessionId);

    return new LoginResponse(
        accessToken,
        refreshToken,
        TOKEN_TYPE,
        jwtTokenProvider.accessTokenExpirationSeconds(),
        new LoginUserResponse(user.getId(), user.getLoginId(), user.getName(), user.getRole()));
  }

  /**
   * 리프레시 토큰 회전(rotate-on-use, 2026-07-27 도입 — docs_h/보안_취약점_점검.md #3). 매 호출마다
   * 기존 세션은 폐기하고 새 리프레시 토큰으로 교체된 세션을 발급한다. 이미 폐기(회전 또는 로그아웃)된
   * 토큰이 다시 제시되면 유출·재사용 공격 신호로 간주해 해당 사용자의 모든 세션을 강제 종료한다.
   */
  @Transactional
  public TokenRefreshResponse refresh(TokenRefreshRequest request, String ipAddress, String userAgent) {
    String refreshTokenHash = tokenHashService.sha256(request.refreshToken());

    AuthSession session =
        authSessionRepository
            .findByRefreshTokenHash(refreshTokenHash)
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

    if (session.getRevokedAt() != null) {
      // 이미 회전되어 폐기됐거나 로그아웃된 토큰의 재사용 — 단순 오류가 아니라 유출 가능성으로 취급한다.
      authSessionRepository.revokeAllByUserId(session.getUserId());
      throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    if (!session.isActive()) {
      // 자연 만료(재사용 신호 아님) — 그냥 거부만 하고 다른 세션에는 영향을 주지 않는다.
      throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    User user =
        userRepository
            .findById(session.getUserId())
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    validateLoginAllowed(user);

    session.revoke();

    String newSessionId = UUID.randomUUID().toString();
    String newRefreshToken = tokenHashService.generateRefreshToken();
    String newRefreshTokenHash = tokenHashService.sha256(newRefreshToken);

    AuthSession rotatedSession =
        AuthSession.create(
            user.getId(),
            newSessionId,
            newRefreshTokenHash,
            ipAddress,
            userAgent,
            OffsetDateTime.now().plusDays(14));

    authSessionRepository.save(rotatedSession);

    String accessToken = jwtTokenProvider.createAccessToken(user, newSessionId);

    return new TokenRefreshResponse(
        accessToken,
        newRefreshToken,
        TOKEN_TYPE,
        jwtTokenProvider.accessTokenExpirationSeconds());
  }

  @Transactional
  public void logout(Long userId, String sessionId, String refreshToken) {
    String refreshTokenHash = tokenHashService.sha256(refreshToken);

    AuthSession session =
        authSessionRepository
            .findByRefreshTokenHash(refreshTokenHash)
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

    if (!session.isOwnedBy(userId, sessionId)) {
      throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    session.revoke();

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    user.recordLogout();
  }

  /** 실패 횟수가 임계치에 도달하는 바로 그 순간에만 잠금을 건다(그 이후는 isLocked에서 먼저 걸러짐). */
  private void recordLoginFailure(String loginId) {
    long failures = rateLimiter.increment(loginFailureKey(loginId), LOGIN_FAILURE_WINDOW);
    if (failures >= LOGIN_MAX_FAILURES) {
      rateLimiter.lock(loginLockKey(loginId), LOGIN_LOCKOUT_DURATION);
    }
  }

  private String loginIpKey(String ipAddress) {
    return "auth:login:ip:" + ipAddress;
  }

  private String loginFailureKey(String loginId) {
    return "auth:login:fail:" + loginId;
  }

  private String loginLockKey(String loginId) {
    return "auth:login:lock:" + loginId;
  }

  private void validateLoginAllowed(User user) {
    if ("PENDING".equals(user.getApprovalStatus())) {
      throw new BusinessException(ErrorCode.SIGNUP_PENDING);
    }

    if ("REJECTED".equals(user.getApprovalStatus())) {
      throw new BusinessException(ErrorCode.SIGNUP_REJECTED);
    }

    if (!"ACTIVE".equals(user.getAccountStatus())) {
      throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
    }
  }
}
