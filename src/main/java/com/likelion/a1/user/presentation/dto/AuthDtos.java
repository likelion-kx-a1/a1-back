package com.likelion.a1.user.presentation.dto;

import java.time.OffsetDateTime;

public final class AuthDtos {
  private AuthDtos() {}

  public record AuthSessionResponse(
      Long id,
      Long userId,
      String sessionId,
      String ipAddress,
      String status,
      OffsetDateTime expiredAt,
      OffsetDateTime revokedAt,
      OffsetDateTime createdAt) {}

  public record EmailVerificationRequest(String email, String purpose) {}

  public record EmailVerificationConfirmRequest(String email, String code, String purpose) {}

  public record PasswordResetRequest(String email) {}

  public record PasswordResetConfirmRequest(String token, String newPassword) {}
}
