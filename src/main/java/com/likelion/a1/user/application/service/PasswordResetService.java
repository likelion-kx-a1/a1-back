package com.likelion.a1.user.application.service;

import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import com.likelion.a1.user.domain.model.User;
import com.likelion.a1.user.domain.repository.AuthSessionRepository;
import com.likelion.a1.user.domain.repository.UserRepository;
import com.likelion.a1.user.presentation.dto.AuthDtos.EmailVerifyRequest;
import com.likelion.a1.user.presentation.dto.AuthDtos.PasswordResetRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {
  private final UserRepository userRepository;
  private final AuthSessionRepository authSessionRepository;
  private final EmailVerificationService emailVerificationService;
  private final PasswordEncoder passwordEncoder;

  public PasswordResetService(
      UserRepository userRepository,
      AuthSessionRepository authSessionRepository,
      EmailVerificationService emailVerificationService,
      PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.authSessionRepository = authSessionRepository;
    this.emailVerificationService = emailVerificationService;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public void reset(PasswordResetRequest request) {
    User user =
        userRepository
            .findByEmail(request.email())
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    emailVerificationService.verify(
        new EmailVerifyRequest(request.email(), request.verificationCode(), "PASSWORD_RESET"));

    user.changePassword(passwordEncoder.encode(request.newPassword()));

    authSessionRepository.revokeAllByUserId(user.getId());

    emailVerificationService.markLatestUsed(request.email(), "PASSWORD_RESET");
  }
}
