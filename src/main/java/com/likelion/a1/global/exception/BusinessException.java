package com.likelion.a1.global.exception;

import java.util.List;

public class BusinessException extends RuntimeException {
  private final ErrorCode errorCode;
  private final List<String> details;

  public BusinessException(ErrorCode errorCode) {
    this(errorCode, List.of());
  }

  public BusinessException(ErrorCode errorCode, List<String> details) {
    super(errorCode.message());
    this.errorCode = errorCode;
    this.details = details;
  }

  public ErrorCode errorCode() {
    return errorCode;
  }

  public List<String> details() {
    return details;
  }
}
