package com.likelion.a1.generation.presentation.dto;

import com.likelion.a1.generation.domain.model.GenerationJob;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
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

  /**
   * highQuality=true면 ByteDance Seedance 2.0, false면 fal.ai Kling O3 Standard 제품군을 호출한다.
   * images 개수(0/1/2장 이상)에 따라 text-to-video / image-to-video / reference-to-video로
   * 자동 분기되므로, jobType이나 modelCode를 직접 지정할 필요가 없다(api_2.md 분기 엔진 규격).
   * refinePrompt는 Claude Sonnet 선행 보정 여부를 선택한다 — null이거나 생략되면 true(보정함)로 간주해
   * 기존 호출자와의 호환성을 지킨다.
   */
  public record VideoGenerationRequest(
      @NotNull Long userId,
      @NotNull Long chatId,
      boolean highQuality,
      List<String> images,
      @NotBlank String prompt,
      Integer duration,
      String aspectRatio,
      Boolean refinePrompt) {}

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
