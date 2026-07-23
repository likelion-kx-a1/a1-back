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
      @NotBlank String instruction,
      Long parentMessageId) {}

  public record ReversePromptRequest(
      @NotNull Long userId,
      @NotNull Long chatId,
      @NotBlank String imageBase64,
      @NotBlank String mimeType,
      @NotBlank String instruction,
      Long parentMessageId) {}

  /**
   * sheetType/sheetValue는 선택 항목이다(api_3.md 시트 주입 엔진 규격). 생략하거나 null이면
   * SheetType.NONE으로 간주되어 기존 호출자(모델 비교 실험, 단독 이미지 생성 등)와 100% 호환된다.
   * - sheetType="CHARACTER_EXPRESSION" + jobType="VIDEO_GENERATION": sheetValue는
   *   HARMONIOUS/CHILLY/MYSTERIOUS/DRAMATIC 중 하나(상황 분위기 코드)
   * - sheetType="CHARACTER_EXPRESSION" + jobType="IMAGE_GENERATION": sheetValue는 사용되지 않음
   *   (9종 감정 3x3 그리드 고정 문구가 결합됨)
   * - sheetType="CUSTOM": sheetValue는 사용자가 직접 편집한 텍스트 그대로
   */
  public record FalJobRequest(
      @NotNull Long userId,
      @NotNull Long chatId,
      @NotBlank String jobType,
      @NotBlank String modelCode,
      String sheetType,
      String sheetValue,
      @NotEmpty Map<String, Object> input,
      Long parentMessageId) {}

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
      Boolean refinePrompt,
      Long parentMessageId) {}

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
