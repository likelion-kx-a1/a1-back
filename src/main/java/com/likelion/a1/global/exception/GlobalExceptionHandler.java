package com.likelion.a1.global.exception;

import java.util.List;
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
    return ResponseEntity.status(code.status()).body(ErrorResponse.of(code, List.of()));
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
}
