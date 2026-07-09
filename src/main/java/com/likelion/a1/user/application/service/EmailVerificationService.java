package com.likelion.a1.user.application.service;

import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import com.likelion.a1.user.application.port.out.EmailSenderPort;
import com.likelion.a1.user.domain.model.EmailVerification;
import com.likelion.a1.user.domain.repository.EmailVerificationRepository;
import com.likelion.a1.user.infrastructure.security.TokenHashService;
import com.likelion.a1.user.presentation.dto.AuthDtos.EmailSendRequest;
import com.likelion.a1.user.presentation.dto.AuthDtos.EmailSendResponse;
import com.likelion.a1.user.presentation.dto.AuthDtos.EmailVerifyRequest;
import com.likelion.a1.user.presentation.dto.AuthDtos.EmailVerifyResponse;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이메일 인증번호 생성, 저장, 발송, 검증을 담당한다.
 *
 * <p>인증번호 원문은 DB에 저장하지 않고 SHA-256 해시만 저장한다.
 */
@Service
public class EmailVerificationService {
  private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
  private static final long VERIFICATION_EXPIRATION_MINUTES = 1;

  private final EmailVerificationRepository emailVerificationRepository;
  private final TokenHashService tokenHashService;
  private final EmailSenderPort emailSenderPort;

  public EmailVerificationService(
      EmailVerificationRepository emailVerificationRepository,
      TokenHashService tokenHashService,
      EmailSenderPort emailSenderPort) {
    this.emailVerificationRepository = emailVerificationRepository;
    this.tokenHashService = tokenHashService;
    this.emailSenderPort = emailSenderPort;
  }

  @Transactional
  public EmailSendResponse send(EmailSendRequest request) {
    String code = tokenHashService.generateVerificationCode();
    String codeHash = tokenHashService.sha256(code);
    OffsetDateTime expiredAt = OffsetDateTime.now().plusMinutes(VERIFICATION_EXPIRATION_MINUTES);

    EmailVerification verification =
        EmailVerification.create(request.email(), codeHash, request.purpose(), expiredAt);

    emailVerificationRepository.save(verification);

    emailSenderPort.sendVerificationCode(request.email(), request.purpose(), code);

    log.info(
        "Email verification code sent. email={}, purpose={}",
        request.email(),
        request.purpose());

    return new EmailSendResponse(expiredAt);
  }

  @Transactional
  public EmailVerifyResponse verify(EmailVerifyRequest request) {
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

    verification.verify();

    return new EmailVerifyResponse(true);
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
