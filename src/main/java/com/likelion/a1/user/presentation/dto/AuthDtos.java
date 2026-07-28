package com.likelion.a1.user.presentation.dto;

import com.likelion.a1.user.domain.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public final class AuthDtos {
  private AuthDtos() {}

  public record LoginIdCheckResponse(boolean available) {}

  public record EmailSendRequest(
      @Email @NotBlank String email,
      @NotBlank
          @Pattern(
              regexp = "SIGNUP|PASSWORD_RESET",
              message = "purpose는 SIGNUP 또는 PASSWORD_RESET이어야 합니다.")
          String purpose) {}

  public record EmailSendResponse(OffsetDateTime expiredAt) {}

  public record EmailVerifyRequest(
      @Email @NotBlank String email,
      @NotBlank
          @Pattern(regexp = "\\d{6}", message = "인증번호는 숫자 6자리여야 합니다.")
          String code,
      @NotBlank
          @Pattern(
              regexp = "SIGNUP|PASSWORD_RESET",
              message = "purpose는 SIGNUP 또는 PASSWORD_RESET이어야 합니다.")
          String purpose) {}

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

  public record SignupStatusResponse(
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

  /** 로그인한 사용자 자신의 프로필 조회(`GET /api/auth/me`) 응답. 관리자 전용 필드(승인자/거절 사유 등)는 제외한다. */
  public record MeResponse(
      Long id,
      String loginId,
      String email,
      String name,
      LocalDate birthDate,
      String phoneNumber,
      String profileImageUrl,
      String role,
      String accountStatus,
      String approvalStatus,
      int loginCount,
      OffsetDateTime lastLoginAt,
      OffsetDateTime createdAt) {
    public static MeResponse from(User user) {
      return new MeResponse(
          user.getId(),
          user.getLoginId(),
          user.getEmail(),
          user.getName(),
          user.getBirthDate(),
          user.getPhoneNumber(),
          user.getProfileImageUrl(),
          user.getRole(),
          user.getAccountStatus(),
          user.getApprovalStatus(),
          user.getLoginCount(),
          user.getLastLoginAt(),
          user.getCreatedAt());
    }
  }

  /**
   * 로그인 상태에서의 비밀번호 변경(`POST /api/auth/password/change`) 요청 — 미인증 forgot-password
   * 흐름(`PasswordResetRequest`)과 달리 이메일 인증 코드 대신 현재 비밀번호로 본인 확인을 한다.
   */
  public record ChangePasswordRequest(
      @NotBlank String currentPassword,
      @NotBlank @Size(min = 8, max = 100) String newPassword) {}
}
