package com.likelion.a1.generation.presentation.dto;

import com.likelion.a1.generation.domain.model.GenerationJob;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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

  public record PromptRequest(
      @NotNull Long userId,
      @NotNull Long chatId,
      String imageBase64,
      String mimeType,
      @NotBlank String instruction) {}

  public record ReversePromptRequest(
      @NotNull Long userId,
      @NotNull Long chatId,
      @NotBlank String imageBase64,
      @NotBlank String mimeType,
      @NotBlank String instruction) {}

  public record FalJobRequest(
      @NotNull Long userId,
      @NotNull Long chatId,
      @NotBlank String jobType,
      @NotBlank String modelCode,
      @NotEmpty Map<String, Object> input) {}

  public record Response(
      Long id,
      Long userId,
      Long chatId,
      Long aiModelId,
      Long requestMessageId,
      String generationType,
      String imageCategory,
      String prompt,
      Map<String, Object> responsePayload,
      String status,
      String errorMessage,
      OffsetDateTime startedAt,
      OffsetDateTime completedAt,
      OffsetDateTime createdAt) {

    public static Response from(GenerationJob job) {
      return new Response(
          job.getId(),
          job.getUserId(),
          job.getChatId(),
          job.getAiModelId(),
          job.getRequestMessageId(),
          job.getGenerationType(),
          job.getImageCategory(),
          job.getPrompt(),
          job.getResponsePayload(),
          job.getStatus(),
          job.getErrorMessage(),
          job.getStartedAt(),
          job.getCompletedAt(),
          job.getCreatedAt());
    }
  }

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
