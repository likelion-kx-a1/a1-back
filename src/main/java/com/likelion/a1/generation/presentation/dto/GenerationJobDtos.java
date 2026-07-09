package com.likelion.a1.generation.presentation.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public final class GenerationJobDtos {
  private GenerationJobDtos() {}

  public record CreateRequest(
      Long chatId,
      Long aiModelId,
      Long requestMessageId,
      String generationType,
      String imageCategory,
      String prompt,
      Map<String, Object> requestPayload) {}

  public record Response(
      Long id,
      Long userId,
      Long chatId,
      Long aiModelId,
      Long requestMessageId,
      String generationType,
      String imageCategory,
      String prompt,
      String status,
      String errorMessage,
      OffsetDateTime startedAt,
      OffsetDateTime completedAt,
      OffsetDateTime createdAt) {}

  public record AiModelResponse(
      Long id,
      String name,
      String provider,
      String modelCode,
      String modelType,
      String description,
      boolean active) {}

  public record QueueResponse(
      Long id,
      Long generationJobId,
      int priority,
      String status,
      int retryCount,
      int maxRetryCount,
      OffsetDateTime availableAt,
      OffsetDateTime lockedAt) {}
}
