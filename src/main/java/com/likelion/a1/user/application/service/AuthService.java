package com.likelion.a1.user.application.service;

import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import com.likelion.a1.user.domain.model.AuthSession;
import com.likelion.a1.user.domain.model.User;
import com.likelion.a1.user.domain.repository.AuthSessionRepository;
import com.likelion.a1.user.domain.repository.UserRepository;
import com.likelion.a1.user.infrastructure.security.JwtTokenProvider;
import com.likelion.a1.user.infrastructure.security.TokenHashService;
import com.likelion.a1.user.presentation.dto.AuthDtos.*;
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

  private final UserRepository userRepository;
  private final AuthSessionRepository authSessionRepository;
  private final EmailVerificationService emailVerificationService;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final TokenHashService tokenHashService;

  public AuthService(
      UserRepository userRepository,
      AuthSessionRepository authSessionRepository,
      EmailVerificationService emailVerificationService,
      PasswordEncoder passwordEncoder,
      JwtTokenProvider jwtTokenProvider,
      TokenHashService tokenHashService) {
    this.userRepository = userRepository;
    this.authSessionRepository = authSessionRepository;
    this.emailVerificationService = emailVerificationService;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenProvider = jwtTokenProvider;
    this.tokenHashService = tokenHashService;
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
    User user =
        userRepository
            .findByLoginId(request.loginId())
            .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_ID_NOT_FOUND));

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new BusinessException(ErrorCode.PASSWORD_NOT_MATCH);
    }

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

  @Transactional(readOnly = true)
  public TokenRefreshResponse refresh(TokenRefreshRequest request) {
    String refreshTokenHash = tokenHashService.sha256(request.refreshToken());

    AuthSession session =
        authSessionRepository
            .findByRefreshTokenHash(refreshTokenHash)
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

    if (!session.isActive()) {
      throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    User user =
        userRepository
            .findById(session.getUserId())
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    validateLoginAllowed(user);

    String accessToken = jwtTokenProvider.createAccessToken(user, session.getSessionId());

    return new TokenRefreshResponse(
        accessToken, TOKEN_TYPE, jwtTokenProvider.accessTokenExpirationSeconds());
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
