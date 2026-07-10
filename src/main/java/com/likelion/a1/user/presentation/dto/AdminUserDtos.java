package com.likelion.a1.user.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class AdminUserDtos {
  private AdminUserDtos() {}

  public record UserSummaryResponse(
      Long id,
      String loginId,
      String email,
      String name,
      String role,
      String accountStatus,
      String approvalStatus,
      int loginCount,
      OffsetDateTime lastLoginAt,
      OffsetDateTime createdAt) {}

  public record UserDetailResponse(
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
      Long approvedBy,
      OffsetDateTime approvedAt,
      OffsetDateTime rejectedAt,
      String rejectionReason,
      int loginCount,
      OffsetDateTime lastLoginAt,
      OffsetDateTime lastLogoutAt,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt,
      OffsetDateTime deletedAt) {}

  public record RejectSignupRequest(@NotBlank String reason) {}

  public record ChangeAccountStatusRequest(
      @NotBlank
          @Pattern(
              regexp = "ACTIVE|INACTIVE",
              message = "accountStatus는 ACTIVE 또는 INACTIVE만 가능합니다.")
          String accountStatus) {}

  public record PageResponse<T>(
      List<T> content, int page, int size, long totalElements, int totalPages) {}
}
