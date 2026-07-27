package com.likelion.a1.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {
  private static final String EMAIL = "tester@example.com";
  private static final String PURPOSE = "SIGNUP";

  @Mock private EmailVerificationRepository emailVerificationRepository;
  @Mock private EmailSenderPort emailSenderPort;
  @Mock private RedisRateLimiter rateLimiter;

  private final TokenHashService tokenHashService = new TokenHashService();

  private EmailVerificationService service;

  @BeforeEach
  void setUp() {
    service =
        new EmailVerificationService(
            emailVerificationRepository, tokenHashService, emailSenderPort, rateLimiter);
  }

  private void stubSendLimitsPass() {
    when(rateLimiter.tryConsume(anyString(), anyLong(), any(Duration.class))).thenReturn(true);
  }

  @Test
  void send_모든_제한을_통과하면_코드를_저장하고_발송한다() {
    stubSendLimitsPass();

    EmailSendResponse response = service.send(new EmailSendRequest(EMAIL, PURPOSE), "127.0.0.1");

    assertThat(response.expiredAt()).isAfter(OffsetDateTime.now());
    verify(emailVerificationRepository).save(any(EmailVerification.class));
    verify(emailSenderPort).sendVerificationCode(eq(EMAIL), eq(PURPOSE), anyString());
    verify(rateLimiter).reset(anyString());
  }

  @Test
  void send_쿨다운을_초과하면_TOO_MANY_REQUESTS이고_메일을_보내지_않는다() {
    when(rateLimiter.tryConsume(anyString(), anyLong(), any(Duration.class))).thenReturn(false);

    assertThatThrownBy(() -> service.send(new EmailSendRequest(EMAIL, PURPOSE), "127.0.0.1"))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).errorCode())
        .isEqualTo(ErrorCode.TOO_MANY_REQUESTS);

    verify(emailSenderPort, never()).sendVerificationCode(any(), any(), any());
    verify(emailVerificationRepository, never()).save(any());
  }

  @Test
  void send_ip가_null이어도_정상_동작한다() {
    stubSendLimitsPass();

    service.send(new EmailSendRequest(EMAIL, PURPOSE), null);

    verify(emailSenderPort).sendVerificationCode(eq(EMAIL), eq(PURPOSE), anyString());
  }

  @Test
  void verify_시도횟수를_초과하면_코드조회_없이_TOO_MANY_REQUESTS() {
    when(rateLimiter.increment(anyString(), any(Duration.class))).thenReturn(6L);

    assertThatThrownBy(() -> service.verify(new EmailVerifyRequest(EMAIL, "123456", PURPOSE)))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).errorCode())
        .isEqualTo(ErrorCode.TOO_MANY_REQUESTS);

    verify(emailVerificationRepository, never()).findLatest(any(), any());
  }

  @Test
  void verify_코드가_존재하지_않으면_EMAIL_VERIFICATION_NOT_FOUND() {
    when(rateLimiter.increment(anyString(), any(Duration.class))).thenReturn(1L);
    when(emailVerificationRepository.findLatest(EMAIL, PURPOSE)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.verify(new EmailVerifyRequest(EMAIL, "123456", PURPOSE)))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).errorCode())
        .isEqualTo(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND);
  }

  @Test
  void verify_만료된_코드는_EMAIL_VERIFICATION_EXPIRED() {
    when(rateLimiter.increment(anyString(), any(Duration.class))).thenReturn(1L);
    EmailVerification expired =
        EmailVerification.create(EMAIL, tokenHashService.sha256("123456"), PURPOSE, OffsetDateTime.now().minusMinutes(1));
    when(emailVerificationRepository.findLatest(EMAIL, PURPOSE)).thenReturn(Optional.of(expired));

    assertThatThrownBy(() -> service.verify(new EmailVerifyRequest(EMAIL, "123456", PURPOSE)))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).errorCode())
        .isEqualTo(ErrorCode.EMAIL_VERIFICATION_EXPIRED);
  }

  @Test
  void verify_이미_사용된_코드는_EMAIL_VERIFICATION_ALREADY_USED() {
    when(rateLimiter.increment(anyString(), any(Duration.class))).thenReturn(1L);
    EmailVerification used =
        EmailVerification.create(EMAIL, tokenHashService.sha256("123456"), PURPOSE, OffsetDateTime.now().plusMinutes(5));
    used.markUsed();
    when(emailVerificationRepository.findLatest(EMAIL, PURPOSE)).thenReturn(Optional.of(used));

    assertThatThrownBy(() -> service.verify(new EmailVerifyRequest(EMAIL, "123456", PURPOSE)))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).errorCode())
        .isEqualTo(ErrorCode.EMAIL_VERIFICATION_ALREADY_USED);
  }

  @Test
  void verify_코드가_불일치하면_EMAIL_VERIFICATION_CODE_NOT_MATCH() {
    when(rateLimiter.increment(anyString(), any(Duration.class))).thenReturn(1L);
    EmailVerification verification =
        EmailVerification.create(EMAIL, tokenHashService.sha256("123456"), PURPOSE, OffsetDateTime.now().plusMinutes(5));
    when(emailVerificationRepository.findLatest(EMAIL, PURPOSE)).thenReturn(Optional.of(verification));

    assertThatThrownBy(() -> service.verify(new EmailVerifyRequest(EMAIL, "000000", PURPOSE)))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).errorCode())
        .isEqualTo(ErrorCode.EMAIL_VERIFICATION_CODE_NOT_MATCH);
  }

  @Test
  void verify_코드가_일치하면_성공하고_시도카운터를_초기화한다() {
    when(rateLimiter.increment(anyString(), any(Duration.class))).thenReturn(1L);
    EmailVerification verification =
        EmailVerification.create(EMAIL, tokenHashService.sha256("123456"), PURPOSE, OffsetDateTime.now().plusMinutes(5));
    when(emailVerificationRepository.findLatest(EMAIL, PURPOSE)).thenReturn(Optional.of(verification));

    EmailVerifyResponse response = service.verify(new EmailVerifyRequest(EMAIL, "123456", PURPOSE));

    assertThat(response.verified()).isTrue();
    assertThat(verification.isVerified()).isTrue();
    verify(rateLimiter).reset(anyString());
  }
}
