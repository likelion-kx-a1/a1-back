package com.likelion.a1.generation.presentation.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public final class GenerationJobDtos {
  private GenerationJobDtos() {}

  public record CreateRequest(
      Long userId,
      Long libraryId,
      Long requestMessageId,
      String modelName,
      String jobType,
      String prompt,
      Map<String, Object> requestPayload) {}

  public record Response(
      Long id,
      Long userId,
      Long libraryId,
      Long requestMessageId,
      String modelName,
      String jobType,
      String prompt,
      String status,
      String errorMessage,
      OffsetDateTime createdAt) {}
}
