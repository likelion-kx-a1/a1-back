package com.likelion.a1.generation.domain.model;

public enum GenerationStatus {
  PENDING,
  QUEUED,
  PROCESSING,
  COMPLETED,
  FAILED,
  CANCELED,
  EXPIRED;

  public static GenerationStatus fromFalStatus(String falStatus) {
    return switch (falStatus) {
      case "IN_QUEUE" -> QUEUED;
      case "IN_PROGRESS" -> PROCESSING;
      case "COMPLETED" -> COMPLETED;
      default -> FAILED;
    };
  }
}
