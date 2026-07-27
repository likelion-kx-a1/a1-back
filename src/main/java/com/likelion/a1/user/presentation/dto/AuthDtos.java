package com.likelion.a1.user.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public final class AuthDtos {
  private AuthDtos() {}

  public record LoginIdCheckResponse(boolean available) {}

  public record EmailSendRequest(
      @Email @NotBlank String email,
      @NotBlank String purpose) {}

  public record EmailSendResponse(OffsetDateTime expiredAt) {}

  public record EmailVerifyRequest(
      @Email @NotBlank String email,
      @NotBlank String code,
      @NotBlank String purpose) {}

  public record EmailVerifyResponse(boolean verified) {}

  public record SignupRequest(
      @NotBlank @Size(min = 4, max = 20) String loginId,
      @Email @NotBlank String email,
      @NotBlank @Size(min = 8, max = 100) String password,
      @NotBlank @Size(min = 2, max = 100) String name,
      @NotNull LocalDate birthDate,
      @NotBlank @Size(max = 30) String phoneNumber) {}

  public record SignupResponse(
      Long userId,
      String approvalStatus,
      String accountStatus) {}

  public record LoginRequest(
      @NotBlank String loginId,
      @NotBlank String password) {}

  public record LoginResponse(
      String accessToken,
      String refreshToken,
      String tokenType,
      long expiresIn,
      LoginUserResponse user) {}

  public record LoginUserResponse(
      Long id,
      String loginId,
      String name,
      String role) {}

  public record TokenRefreshRequest(@NotBlank String refreshToken) {}

  /**
   * refreshToken은 rotate-on-use 정책(2026-07-27 도입)에 따라 매 호출마다 새로 발급된다 — 클라이언트는
   * 이 응답의 refreshToken으로 이전에 갖고 있던 값을 반드시 교체 저장해야 하며, 다음 재발급 요청에는
   * 새 값을 사용해야 한다. 이미 교체되어 폐기된(rotate-out) refreshToken을 다시 사용하면 재사용 공격으로
   * 간주되어 해당 사용자의 모든 세션이 강제 종료된다.
   */
  public record TokenRefreshResponse(
      String accessToken,
      String refreshToken,
      String tokenType,
      long expiresIn) {}

  public record LogoutRequest(@NotBlank String refreshToken) {}

  public record PasswordResetRequest(
      @Email @NotBlank String email,
      @NotBlank String verificationCode,
      @NotBlank @Size(min = 8, max = 100) String newPassword) {}
}
