package com.likelion.a1.user.application.service;

import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import com.likelion.a1.user.application.port.out.EmailSenderPort;
import com.likelion.a1.user.domain.model.EmailVerification;
import com.likelion.a1.user.domain.repository.EmailVerificationRepository;
import com.likelion.a1.user.infrastructure.ratelimit.RedisRateLimiter;
import com.likelion.a1.user.infrastructure.security.TokenHashService;
import com.likelion.a1.user.presentation.dto.AuthDtos.EmailSendRequest;
import com.likelion.a1.user.presentation.dto.AuthDtos.EmailSendResponse;
import com.likelion.a1.user.presentation.dto.AuthDtos.EmailVerifyRequest;
import com.likelion.a1.user.presentation.dto.AuthDtos.EmailVerifyResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이메일 인증번호 생성, 저장, 발송, 검증을 담당한다.
 *
 * <p>인증번호 원문은 DB에 저장하지 않고 SHA-256 해시만 저장한다.
 *
 * <p>발송/검증 모두 Redis 기반 레이트리밋이 걸려 있다(docs_h/보안_취약점_점검.md #5) — 6자리 코드는
 * 100만 가지뿐이라 시도 횟수 제한 없이는 코드 만료(5분) 안에도 총력 대입이 현실적으로 가능하고, 발송
 * 자체도 제한이 없으면 메일 폭탄/스팸 대상으로 악용될 수 있다.
 */
@Service
public class EmailVerificationService {
  private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
  private static final long VERIFICATION_EXPIRATION_MINUTES = 5;

  private static final Duration EMAIL_SEND_COOLDOWN = Duration.ofSeconds(60);
  private static final long EMAIL_SEND_MAX_PER_HOUR = 5;
  private static final long EMAIL_SEND_IP_MAX_PER_HOUR = 20;
  private static final long EMAIL_VERIFY_MAX_ATTEMPTS = 5;
  private static final Duration VERIFY_ATTEMPT_WINDOW = Duration.ofMinutes(VERIFICATION_EXPIRATION_MINUTES);

  private final EmailVerificationRepository emailVerificationRepository;
  private final TokenHashService tokenHashService;
  private final EmailSenderPort emailSenderPort;
  private final RedisRateLimiter rateLimiter;

  public EmailVerificationService(
      EmailVerificationRepository emailVerificationRepository,
      TokenHashService tokenHashService,
      EmailSenderPort emailSenderPort,
      RedisRateLimiter rateLimiter) {
    this.emailVerificationRepository = emailVerificationRepository;
    this.tokenHashService = tokenHashService;
    this.emailSenderPort = emailSenderPort;
    this.rateLimiter = rateLimiter;
  }

  @Transactional
  public EmailSendResponse send(EmailSendRequest request, String ipAddress) {
    enforceSendRateLimit(request.email(), request.purpose(), ipAddress);

    String code = tokenHashService.generateVerificationCode();
    String codeHash = tokenHashService.sha256(code);
    OffsetDateTime expiredAt = OffsetDateTime.now().plusMinutes(VERIFICATION_EXPIRATION_MINUTES);

    EmailVerification verification =
        EmailVerification.create(request.email(), codeHash, request.purpose(), expiredAt);

    emailVerificationRepository.save(verification);

    // 새 코드를 보내는 순간 예전 코드에 대한 검증 시도 실패 카운트는 의미가 없어지므로 초기화한다.
    rateLimiter.reset(verifyAttemptKey(request.email(), request.purpose()));

    emailSenderPort.sendVerificationCode(request.email(), request.purpose(), code);

    log.info(
        "Email verification code sent. email={}, purpose={}",
        request.email(),
        request.purpose());

    return new EmailSendResponse(expiredAt);
  }

  @Transactional
  public EmailVerifyResponse verify(EmailVerifyRequest request) {
    String attemptKey = verifyAttemptKey(request.email(), request.purpose());
    if (rateLimiter.increment(attemptKey, VERIFY_ATTEMPT_WINDOW) > EMAIL_VERIFY_MAX_ATTEMPTS) {
      throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
    }

    EmailVerification verification =
        emailVerificationRepository
            .findLatest(request.email(), request.purpose())
            .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND));

    if (verification.isExpired()) {
      throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_EXPIRED);
    }

    if (verification.isUsed()) {
      throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_ALREADY_USED);
    }

    String inputCodeHash = tokenHashService.sha256(request.code());

    if (!inputCodeHash.equals(verification.getVerificationCodeHash())) {
      throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_CODE_NOT_MATCH);
    }

    rateLimiter.reset(attemptKey);
    verification.verify();

    return new EmailVerifyResponse(true);
  }

  private void enforceSendRateLimit(String email, String purpose, String ipAddress) {
    String cooldownKey = "auth:email-send:cooldown:" + purpose + ":" + email;
    if (!rateLimiter.tryConsume(cooldownKey, 1, EMAIL_SEND_COOLDOWN)) {
      throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
    }

    String hourlyKey = "auth:email-send:hourly:" + purpose + ":" + email;
    if (!rateLimiter.tryConsume(hourlyKey, EMAIL_SEND_MAX_PER_HOUR, Duration.ofHours(1))) {
      throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
    }

    if (ipAddress != null) {
      String ipKey = "auth:email-send:ip:" + ipAddress;
      if (!rateLimiter.tryConsume(ipKey, EMAIL_SEND_IP_MAX_PER_HOUR, Duration.ofHours(1))) {
        throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
      }
    }
  }

  private String verifyAttemptKey(String email, String purpose) {
    return "auth:email-verify:fail:" + purpose + ":" + email;
  }

  @Transactional(readOnly = true)
  public void validateVerified(String email, String purpose) {
    EmailVerification verification =
        emailVerificationRepository
            .findLatest(email, purpose)
            .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_VERIFICATION_REQUIRED));

    if (!verification.isVerified() || verification.isExpired() || verification.isUsed()) {
      throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_REQUIRED);
    }
  }

  @Transactional
  public void markLatestUsed(String email, String purpose) {
    EmailVerification verification =
        emailVerificationRepository
            .findLatest(email, purpose)
            .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_VERIFICATION_REQUIRED));

    if (!verification.isVerified() || verification.isExpired() || verification.isUsed()) {
      throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_REQUIRED);
    }

    verification.markUsed();
  }
}
