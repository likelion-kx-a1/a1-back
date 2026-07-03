package com.likelion.a1.user.presentation.dto;

import com.likelion.a1.user.domain.model.User;

public record UserResponse(Long id, String email, String nickname, String role, String status) {
  public static UserResponse from(User user) {
    return new UserResponse(
        user.getId(), user.getEmail(), user.getNickname(), user.getRole(), user.getStatus());
  }
}
