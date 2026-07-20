package com.likelion.a1.global.exception;

import java.util.List;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(BusinessException.class)
  ResponseEntity<ErrorResponse> handleBusiness(BusinessException exception) {
    ErrorCode code = exception.errorCode();
    return ResponseEntity.status(code.status()).body(ErrorResponse.of(code, exception.details()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
    List<String> details =
        exception.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .toList();
    return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.INVALID_INPUT, details));
  }

  @ExceptionHandler(RestClientResponseException.class)
  ResponseEntity<ErrorResponse> handleAiProviderFailure(RestClientResponseException exception) {
    ErrorCode code = ErrorCode.AI_PROVIDER_REQUEST_FAILED;
    List<String> details =
        List.of(exception.getStatusCode() + ": " + exception.getResponseBodyAsString());
    return ResponseEntity.status(code.status()).body(ErrorResponse.of(code, details));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
    ErrorCode code = resolveConstraintErrorCode(constraintName(exception));
    return ResponseEntity.status(code.status()).body(ErrorResponse.of(code, List.of()));
  }

  private String constraintName(DataIntegrityViolationException exception) {
    Throwable cause = exception.getMostSpecificCause();
    if (cause instanceof ConstraintViolationException constraintViolationException) {
      return constraintViolationException.getConstraintName();
    }
    return cause.getMessage();
  }

  private ErrorCode resolveConstraintErrorCode(String constraintName) {
    if (constraintName == null) {
      return ErrorCode.INVALID_INPUT;
    }
    if (constraintName.contains("user_id")) {
      return ErrorCode.USER_NOT_FOUND;
    }
    if (constraintName.contains("chat_id") || constraintName.contains("library_id")) {
      return ErrorCode.CHAT_NOT_FOUND;
    }
    return ErrorCode.INVALID_INPUT;
  }
}
