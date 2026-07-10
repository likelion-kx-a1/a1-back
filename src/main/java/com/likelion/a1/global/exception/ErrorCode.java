package com.likelion.a1.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
  // 공통 에러 코드
  INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON-001", "입력값이 올바르지 않습니다."),
  USER_EMAIL_DUPLICATE(HttpStatus.CONFLICT, "USER-001", "이미 사용 중인 이메일입니다."),
  GENERATION_NOT_FOUND(HttpStatus.NOT_FOUND, "GENERATION-001", "생성 작업을 찾을 수 없습니다."),
  INVALID_GENERATION_STATE(HttpStatus.CONFLICT, "GENERATION-002", "현재 상태에서는 요청을 처리할 수 없습니다."),
  AI_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "PROVIDER-001", "AI 공급자가 설정되지 않았습니다."),
  USER_LOGIN_ID_DUPLICATE(HttpStatus.CONFLICT, "USER_LOGIN_ID_DUPLICATE", "이미 사용 중인 아이디입니다."),
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
  // 회원가입 및 로그인 관련 에러 코드
  LOGIN_ID_NOT_FOUND(HttpStatus.UNAUTHORIZED, "LOGIN_ID_NOT_FOUND", "아이디가 일치하지 않습니다."),
  PASSWORD_NOT_MATCH(HttpStatus.UNAUTHORIZED, "PASSWORD_NOT_MATCH", "비밀번호가 일치하지 않습니다."),
  SIGNUP_PENDING(HttpStatus.FORBIDDEN, "SIGNUP_PENDING", "관리자 승인 후 이용 가능합니다."),
  SIGNUP_REJECTED(HttpStatus.FORBIDDEN, "SIGNUP_REJECTED", "회원가입 신청이 거절되었습니다."),
  ACCOUNT_INACTIVE(HttpStatus.FORBIDDEN, "ACCOUNT_INACTIVE", "계정 상태가 비활성화입니다."),
  // JWT 관련 에러 코드
  INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_ACCESS_TOKEN", "유효하지 않은 Access Token입니다."),
  INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "유효하지 않은 Refresh Token입니다."),
  EMAIL_VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "EMAIL_VERIFICATION_NOT_FOUND", "이메일 인증 요청을 찾을 수 없습니다."),
  EMAIL_VERIFICATION_EXPIRED(HttpStatus.BAD_REQUEST, "EMAIL_VERIFICATION_EXPIRED", "이메일 인증번호가 만료되었습니다."),
  EMAIL_VERIFICATION_CODE_NOT_MATCH(HttpStatus.BAD_REQUEST, "EMAIL_VERIFICATION_CODE_NOT_MATCH", "이메일 인증번호가 일치하지 않습니다."),
  EMAIL_VERIFICATION_ALREADY_USED(HttpStatus.BAD_REQUEST, "EMAIL_VERIFICATION_ALREADY_USED", "이미 사용된 이메일 인증번호입니다."),
  EMAIL_VERIFICATION_REQUIRED(HttpStatus.BAD_REQUEST, "EMAIL_VERIFICATION_REQUIRED", "이메일 인증이 필요합니다."),
  INVALID_ACCOUNT_STATUS(HttpStatus.BAD_REQUEST, "INVALID_ACCOUNT_STATUS", "유효하지 않은 계정 상태입니다."),
  INVALID_APPROVAL_STATUS(HttpStatus.BAD_REQUEST, "INVALID_APPROVAL_STATUS", "유효하지 않은 가입 승인 상태입니다."),
  SIGNUP_ALREADY_PROCESSED(HttpStatus.CONFLICT, "SIGNUP_ALREADY_PROCESSED", "이미 처리된 회원가입 요청입니다."),
  ADMIN_CANNOT_UPDATE_SELF(HttpStatus.BAD_REQUEST, "ADMIN_CANNOT_UPDATE_SELF", "관리자는 본인 계정을 직접 변경할 수 없습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;

  ErrorCode(HttpStatus status, String code, String message) {
    this.status = status;
    this.code = code;
    this.message = message;
  }

  public HttpStatus status() {
    return status;
  }

  public String code() {
    return code;
  }

  public String message() {
    return message;
  }
}
