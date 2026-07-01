package com.likelion.a1.global.exception;

import java.time.OffsetDateTime;
import java.util.List;

public record ErrorResponse(boolean success, Error error, OffsetDateTime timestamp) {
    public record Error(String code, String message, List<String> details) {}

    public static ErrorResponse of(ErrorCode code, List<String> details) {
        return new ErrorResponse(false, new Error(code.code(), code.message(), details), OffsetDateTime.now());
    }
}
