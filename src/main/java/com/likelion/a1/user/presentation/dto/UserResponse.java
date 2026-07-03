package com.likelion.a1.user.presentation.dto;

import com.likelion.a1.user.domain.model.User;
import java.util.UUID;

public record UserResponse(Long id, UUID publicId, String email, String nickname) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getPublicId(), user.getEmail(), user.getNickname());
    }
}
