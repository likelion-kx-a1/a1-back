package com.likelion.a1.global.response;

import java.time.OffsetDateTime;

public record ApiResponse<T>(boolean success, String code, String message, T data, OffsetDateTime timestamp) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "SUCCESS", "요청이 성공했습니다.", data, OffsetDateTime.now());
    }
}
