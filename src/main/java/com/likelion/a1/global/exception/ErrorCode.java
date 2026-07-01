package com.likelion.a1.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON-001", "입력값이 올바르지 않습니다."),
    USER_EMAIL_DUPLICATE(HttpStatus.CONFLICT, "USER-001", "이미 사용 중인 이메일입니다."),
    GENERATION_NOT_FOUND(HttpStatus.NOT_FOUND, "GENERATION-001", "생성 작업을 찾을 수 없습니다."),
    INVALID_GENERATION_STATE(HttpStatus.CONFLICT, "GENERATION-002", "현재 상태에서는 요청을 처리할 수 없습니다."),
    AI_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "PROVIDER-001", "AI 공급자가 설정되지 않았습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus status() { return status; }
    public String code() { return code; }
    public String message() { return message; }
}
