package com.likelion.a1.user.presentation.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.likelion.a1.user.presentation.dto.AuthDtos.EmailSendRequest;
import com.likelion.a1.user.presentation.dto.AuthDtos.EmailVerifyRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AuthDtosValidationTest {
  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void 이메일_인증_목적은_지원하는_값만_허용한다() {
    EmailSendRequest request = new EmailSendRequest("tester@example.com", "UNKNOWN");

    assertThat(validator.validate(request))
        .anyMatch(violation -> violation.getPropertyPath().toString().equals("purpose"));
  }

  @Test
  void 이메일_인증번호는_숫자_6자리만_허용한다() {
    EmailVerifyRequest request =
        new EmailVerifyRequest("tester@example.com", "12A45", "SIGNUP");

    assertThat(validator.validate(request))
        .anyMatch(violation -> violation.getPropertyPath().toString().equals("code"));
  }

  @Test
  void 회원가입_이메일_인증_요청_형식을_허용한다() {
    EmailVerifyRequest request =
        new EmailVerifyRequest("tester@example.com", "123456", "SIGNUP");

    assertThat(validator.validate(request)).isEmpty();
  }
}
