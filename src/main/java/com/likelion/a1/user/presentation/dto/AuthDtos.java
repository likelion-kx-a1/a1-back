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

  public record TokenRefreshResponse(
      String accessToken,
      String tokenType,
      long expiresIn) {}

  public record LogoutRequest(@NotBlank String refreshToken) {}

  public record PasswordResetRequest(
      @Email @NotBlank String email,
      @NotBlank String verificationCode,
      @NotBlank @Size(min = 8, max = 100) String newPassword) {}
}
