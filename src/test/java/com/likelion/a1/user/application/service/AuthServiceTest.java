package com.likelion.a1.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import com.likelion.a1.user.domain.model.AuthSession;
import com.likelion.a1.user.domain.model.User;
import com.likelion.a1.user.domain.repository.AuthSessionRepository;
import com.likelion.a1.user.domain.repository.UserRepository;
import com.likelion.a1.user.infrastructure.ratelimit.RedisRateLimiter;
import com.likelion.a1.user.infrastructure.security.JwtTokenProvider;
import com.likelion.a1.user.infrastructure.security.TokenHashService;
import com.likelion.a1.user.presentation.dto.AuthDtos.LoginRequest;
import com.likelion.a1.user.presentation.dto.AuthDtos.LoginResponse;
import com.likelion.a1.user.presentation.dto.AuthDtos.TokenRefreshRequest;
import com.likelion.a1.user.presentation.dto.AuthDtos.TokenRefreshResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
  private static final String LOGIN_ID = "tester";
  private static final String RAW_PASSWORD = "correct-password";
  private static final String IP = "127.0.0.1";

  @Mock private UserRepository userRepository;
  @Mock private AuthSessionRepository authSessionRepository;
  @Mock private EmailVerificationService emailVerificationService;
  @Mock private RedisRateLimiter rateLimiter;

  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
  private final JwtTokenProvider jwtTokenProvider =
      new JwtTokenProvider("test-secret-key-must-be-at-least-32-bytes-long", 3600);
  private final TokenHashService tokenHashService = new TokenHashService();

  private AuthService authService;

  @BeforeEach
  void setUp() {
    authService =
        new AuthService(
            userRepository,
            authSessionRepository,
            emailVerificationService,
            passwordEncoder,
            jwtTokenProvider,
            tokenHashService,
            rateLimiter);
  }

  private User activeUser() {
    User user =
        User.signup(
            LOGIN_ID,
            "tester@example.com",
            passwordEncoder.encode(RAW_PASSWORD),
            "테스터",
            LocalDate.of(2000, 1, 1),
            "010-0000-0000");
    user.approve(1L);
    return user;
  }

  private void stubIpAndLockPass() {
    when(rateLimiter.tryConsume(anyString(), anyLong(), any(Duration.class))).thenReturn(true);
    when(rateLimiter.isLocked(anyString())).thenReturn(false);
  }

  @Test
  void login_성공하면_토큰과_세션을_발급하고_실패카운터를_초기화한다() {
    stubIpAndLockPass();
    User user = activeUser();
    when(userRepository.findByLoginId(LOGIN_ID)).thenReturn(Optional.of(user));

    LoginResponse response = authService.login(new LoginRequest(LOGIN_ID, RAW_PASSWORD), IP, "ua");

    assertThat(response.accessToken()).isNotBlank();
    assertThat(response.refreshToken()).isNotBlank();
    verify(authSessionRepository).save(any(AuthSession.class));
    verify(rateLimiter).reset(anyString());
    verify(rateLimiter, never()).increment(anyString(), any());
  }

  @Test
  void login_IP_레이트리밋을_초과하면_계정_조회_전에_거부된다() {
    when(rateLimiter.tryConsume(anyString(), anyLong(), any(Duration.class))).thenReturn(false);

    assertThatThrownBy(() -> authService.login(new LoginRequest(LOGIN_ID, RAW_PASSWORD), IP, "ua"))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).errorCode())
        .isEqualTo(ErrorCode.TOO_MANY_REQUESTS);

    verify(userRepository, never()).findByLoginId(anyString());
  }

  @Test
  void login_존재하지_않는_아이디는_LOGIN_ID_NOT_FOUND() {
    when(rateLimiter.tryConsume(anyString(), anyLong(), any(Duration.class))).thenReturn(true);
    when(userRepository.findByLoginId(LOGIN_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.login(new LoginRequest(LOGIN_ID, RAW_PASSWORD), IP, "ua"))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).errorCode())
        .isEqualTo(ErrorCode.LOGIN_ID_NOT_FOUND);
  }

  @Test
  void login_잠긴_계정은_비밀번호_확인_없이_ACCOUNT_LOCKED() {
    when(rateLimiter.tryConsume(anyString(), anyLong(), any(Duration.class))).thenReturn(true);
    when(userRepository.findByLoginId(LOGIN_ID)).thenReturn(Optional.of(activeUser()));
    when(rateLimiter.isLocked(anyString())).thenReturn(true);

    assertThatThrownBy(
            () -> authService.login(new LoginRequest(LOGIN_ID, "wrong-password"), IP, "ua"))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).errorCode())
        .isEqualTo(ErrorCode.ACCOUNT_LOCKED);
  }

  @Test
  void login_비밀번호가_틀리면_실패카운터를_증가시키고_PASSWORD_NOT_MATCH() {
    stubIpAndLockPass();
    when(userRepository.findByLoginId(LOGIN_ID)).thenReturn(Optional.of(activeUser()));
    when(rateLimiter.increment(anyString(), any(Duration.class))).thenReturn(3L);

    assertThatThrownBy(
            () -> authService.login(new LoginRequest(LOGIN_ID, "wrong-password"), IP, "ua"))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).errorCode())
        .isEqualTo(ErrorCode.PASSWORD_NOT_MATCH);

    verify(rateLimiter).increment(anyString(), any(Duration.class));
    verify(rateLimiter, never()).lock(anyString(), any());
  }

  @Test
  void login_실패_횟수가_임계치에_도달하면_계정을_잠근다() {
    stubIpAndLockPass();
    when(userRepository.findByLoginId(LOGIN_ID)).thenReturn(Optional.of(activeUser()));
    when(rateLimiter.increment(anyString(), any(Duration.class))).thenReturn(5L);

    assertThatThrownBy(
            () -> authService.login(new LoginRequest(LOGIN_ID, "wrong-password"), IP, "ua"))
        .isInstanceOf(BusinessException.class);

    verify(rateLimiter).lock(anyString(), eq(Duration.ofMinutes(15)));
  }

  @Test
  void refresh_유효한_세션이면_토큰을_회전시키고_이전_세션을_폐기한다() {
    User user = activeUser();
    setId(user, 1L);
    AuthSession oldSession =
        AuthSession.create(1L, "old-session", "old-hash", IP, "ua", OffsetDateTime.now().plusDays(1));

    when(authSessionRepository.findByRefreshTokenHash(anyString())).thenReturn(Optional.of(oldSession));
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    TokenRefreshResponse response =
        authService.refresh(new TokenRefreshRequest("old-refresh-token"), IP, "ua");

    assertThat(oldSession.getRevokedAt()).isNotNull();
    assertThat(response.refreshToken()).isNotEqualTo("old-refresh-token");

    ArgumentCaptor<AuthSession> savedSession = ArgumentCaptor.forClass(AuthSession.class);
    verify(authSessionRepository).save(savedSession.capture());
    assertThat(savedSession.getValue().getSessionId()).isNotEqualTo("old-session");
    verify(authSessionRepository, never()).revokeAllByUserId(any());
  }

  @Test
  void refresh_이미_폐기된_토큰이_재사용되면_전체_세션을_강제종료한다() {
    AuthSession revokedSession =
        AuthSession.create(1L, "old-session", "old-hash", IP, "ua", OffsetDateTime.now().plusDays(1));
    revokedSession.revoke();

    when(authSessionRepository.findByRefreshTokenHash(anyString())).thenReturn(Optional.of(revokedSession));

    assertThatThrownBy(() -> authService.refresh(new TokenRefreshRequest("stolen-token"), IP, "ua"))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).errorCode())
        .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);

    verify(authSessionRepository).revokeAllByUserId(1L);
    verify(authSessionRepository, never()).save(any());
  }

  @Test
  void refresh_자연만료된_토큰은_다른_세션에_영향을_주지_않고_거부만_한다() {
    AuthSession expiredSession =
        AuthSession.create(1L, "old-session", "old-hash", IP, "ua", OffsetDateTime.now().minusDays(1));

    when(authSessionRepository.findByRefreshTokenHash(anyString())).thenReturn(Optional.of(expiredSession));

    assertThatThrownBy(() -> authService.refresh(new TokenRefreshRequest("expired-token"), IP, "ua"))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).errorCode())
        .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);

    verify(authSessionRepository, never()).revokeAllByUserId(any());
    verify(authSessionRepository, never()).save(any());
  }

  @Test
  void logout_소유하지_않은_세션이면_거부한다() {
    AuthSession session =
        AuthSession.create(1L, "session-a", "hash", IP, "ua", OffsetDateTime.now().plusDays(1));
    when(authSessionRepository.findByRefreshTokenHash(anyString())).thenReturn(Optional.of(session));

    assertThatThrownBy(() -> authService.logout(999L, "session-a", "refresh-token"))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).errorCode())
        .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
  }

  @Test
  void logout_정상_소유자면_세션을_폐기한다() {
    AuthSession session =
        AuthSession.create(1L, "session-a", "hash", IP, "ua", OffsetDateTime.now().plusDays(1));
    User user = activeUser();
    setId(user, 1L);

    when(authSessionRepository.findByRefreshTokenHash(anyString())).thenReturn(Optional.of(session));
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    authService.logout(1L, "session-a", "refresh-token");

    assertThat(session.getRevokedAt()).isNotNull();
  }

  @Test
  void changePassword_현재_비밀번호가_틀리면_거부하고_세션을_건드리지_않는다() {
    User user = activeUser();
    setId(user, 1L);
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> authService.changePassword(1L, "session-a", "wrong-password", "new-password"))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).errorCode())
        .isEqualTo(ErrorCode.PASSWORD_NOT_MATCH);

    verify(authSessionRepository, never()).revokeAllByUserIdExceptSessionId(any(), any());
  }

  @Test
  void changePassword_성공하면_비밀번호를_바꾸고_다른_세션만_종료한다() {
    User user = activeUser();
    setId(user, 1L);
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    authService.changePassword(1L, "session-a", RAW_PASSWORD, "new-password-123");

    assertThat(passwordEncoder.matches("new-password-123", user.getPasswordHash())).isTrue();
    verify(authSessionRepository).revokeAllByUserIdExceptSessionId(1L, "session-a");
  }

  @Test
  void logoutAll은_해당_사용자의_모든_세션을_종료한다() {
    authService.logoutAll(1L);

    verify(authSessionRepository, times(1)).revokeAllByUserId(1L);
  }

  @Test
  void getMe는_존재하지_않는_사용자면_USER_NOT_FOUND() {
    when(userRepository.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.getMe(1L))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).errorCode())
        .isEqualTo(ErrorCode.USER_NOT_FOUND);
  }

  /** User.id는 @GeneratedValue라 리플렉션 없이는 테스트에서 채울 수 없어 헬퍼로 강제 주입한다. */
  private void setId(User user, Long id) {
    try {
      var field = User.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(user, id);
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
