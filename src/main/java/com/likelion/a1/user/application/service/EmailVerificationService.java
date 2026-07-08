package com.likelion.a1.user.application.service;

import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import com.likelion.a1.user.domain.model.EmailVerification;
import com.likelion.a1.user.domain.repository.EmailVerificationRepository;
import com.likelion.a1.user.infrastructure.security.TokenHashService;
import com.likelion.a1.user.presentation.dto.AuthDtos.*;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
    인증번호 생성
    인증번호 hash 저장
    인증번호 검증
    회원가입 전에 인증 완료 여부 확인
    비밀번호 재설정 전에 인증 완료 여부 확인
 */
@Service
public class EmailVerificationService {
  private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
  private static final long VERIFICATION_EXPIRATION_MINUTES = 1;

  private final EmailVerificationRepository emailVerificationRepository;
  private final TokenHashService tokenHashService;

  public EmailVerificationService(
      EmailVerificationRepository emailVerificationRepository,
      TokenHashService tokenHashService) {
    this.emailVerificationRepository = emailVerificationRepository;
    this.tokenHashService = tokenHashService;
  }

  @Transactional
  public EmailSendResponse send(EmailSendRequest request) {
    String code = tokenHashService.generateVerificationCode();
    String codeHash = tokenHashService.sha256(code);
    OffsetDateTime expiredAt = OffsetDateTime.now().plusMinutes(VERIFICATION_EXPIRATION_MINUTES);

    EmailVerification verification =
        EmailVerification.create(request.email(), codeHash, request.purpose(), expiredAt);

    emailVerificationRepository.save(verification);

    log.info(
        "Email verification code. email={}, purpose={}, code={}",
        request.email(),
        request.purpose(),
        code);

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
