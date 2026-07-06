package com.likelion.a1.user.presentation.dto;

import com.likelion.a1.user.domain.model.User;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record UserResponse(
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
    OffsetDateTime createdAt) {
  public static UserResponse from(User user) {
    return new UserResponse(
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
        user.getApprovedBy(),
        user.getApprovedAt(),
        user.getCreatedAt());
  }
}
