package com.likelion.a1.generation.application.service;

import com.likelion.a1.generation.application.port.out.AiTextGenerationResult;
import com.likelion.a1.generation.application.port.out.FalGenerationPort;
import com.likelion.a1.generation.application.port.out.FalGenerationStatus;
import com.likelion.a1.generation.application.port.out.FalGenerationSubmission;
import com.likelion.a1.generation.application.port.out.ImageAnalysisPort;
import com.likelion.a1.generation.application.port.out.PromptGenerationPort;
import com.likelion.a1.generation.domain.model.GenerationJob;
import com.likelion.a1.generation.domain.model.GenerationStatus;
import com.likelion.a1.generation.domain.model.GenerationType;
import com.likelion.a1.generation.domain.repository.GenerationJobRepository;
import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;

/** 프롬프트 보정, 역프롬프트 추출, fal.ai 비동기 제출/폴링 전체 파이프라인을 조율하고 GenerationJob으로 영속화한다. */
@Service
@Transactional
public class GenerationAiService {
  private static final int MAX_REFERENCE_IMAGES = 9;

  private final GenerationJobRepository generationJobRepository;
  private final PromptGenerationPort promptGenerationPort;
  private final ImageAnalysisPort imageAnalysisPort;
  private final FalGenerationPort falGenerationPort;
  private final GeneratedMediaUploader generatedMediaUploader;

  public GenerationAiService(
      GenerationJobRepository generationJobRepository,
      PromptGenerationPort promptGenerationPort,
      ImageAnalysisPort imageAnalysisPort,
      FalGenerationPort falGenerationPort,
      GeneratedMediaUploader generatedMediaUploader) {
    this.generationJobRepository = generationJobRepository;
    this.promptGenerationPort = promptGenerationPort;
    this.imageAnalysisPort = imageAnalysisPort;
    this.falGenerationPort = falGenerationPort;
    this.generatedMediaUploader = generatedMediaUploader;
  }

  public GenerationJob regeneratePrompt(
      Long userId, Long chatId, String imageBase64, String mimeType, String instruction) {
    byte[] imageBytes = imageBase64 == null || imageBase64.isBlank() ? null : decodeImage(imageBase64);

    Map<String, Object> requestPayload = new LinkedHashMap<>();
    requestPayload.put("mimeType", mimeType);

    GenerationJob job =
        generationJobRepository.save(
            GenerationJob.create(
                userId,
                chatId,
                null,
                null,
                GenerationType.PROMPT_REGENERATION.name(),
                instruction,
                requestPayload));

    try {
      long startedAtMs = System.currentTimeMillis();
      AiTextGenerationResult result = promptGenerationPort.generateFromImage(imageBytes, mimeType, instruction);
      long refineDurationMs = System.currentTimeMillis() - startedAtMs;

      Map<String, Object> responsePayload = toResponsePayload(result);
      PerformanceMetrics.record(responsePayload, "refineDurationMs", refineDurationMs);
      PerformanceMetrics.announce(job.getId(), responsePayload);
      job.complete(responsePayload);
    } catch (RestClientResponseException exception) {
      job.fail(exception.getMessage());
      generationJobRepository.save(job);
      throw exception;
    }

    return generationJobRepository.save(job);
  }

  public GenerationJob reversePrompt(
      Long userId, Long chatId, String imageBase64, String mimeType, String instruction) {
    byte[] imageBytes = decodeImage(imageBase64);

    Map<String, Object> requestPayload = new LinkedHashMap<>();
    requestPayload.put("mimeType", mimeType);

    GenerationJob job =
        generationJobRepository.save(
            GenerationJob.create(
                userId, chatId, null, null, GenerationType.REVERSE_PROMPT.name(), instruction, requestPayload));

    try {
      long startedAtMs = System.currentTimeMillis();
      AiTextGenerationResult result = imageAnalysisPort.analyze(imageBytes, mimeType, instruction);
      long analysisDurationMs = System.currentTimeMillis() - startedAtMs;

      Map<String, Object> responsePayload = toResponsePayload(result);
      PerformanceMetrics.record(responsePayload, "analysisDurationMs", analysisDurationMs);
      PerformanceMetrics.announce(job.getId(), responsePayload);
      job.complete(responsePayload);
    } catch (RestClientResponseException exception) {
      job.fail(exception.getMessage());
      generationJobRepository.save(job);
      throw exception;
    }

    return generationJobRepository.save(job);
  }

  public GenerationJob submitFalJob(
      Long userId, Long chatId, String jobType, String modelCode, Map<String, Object> input) {
    GenerationType type = parseGenerationType(jobType);
    String prompt = input.get("prompt") instanceof String promptText ? promptText : null;

    Map<String, Object> requestPayload = new LinkedHashMap<>();
    requestPayload.put("modelCode", modelCode);
    requestPayload.put("input", input);

    GenerationJob job =
        generationJobRepository.save(
            GenerationJob.create(userId, chatId, null, null, type.name(), prompt, requestPayload));

    try {
      long startedAtMs = System.currentTimeMillis();
      FalGenerationSubmission submission = falGenerationPort.submit(modelCode, input);
      long submissionLatencyMs = System.currentTimeMillis() - startedAtMs;

      Map<String, Object> responsePayload = new LinkedHashMap<>();
      responsePayload.put("requestId", submission.externalRequestId());
      responsePayload.put("statusUrl", submission.statusUrl());
      responsePayload.put("responseUrl", submission.responseUrl());
      responsePayload.put("raw", submission.rawResponse());
      PerformanceMetrics.record(responsePayload, "submissionLatencyMs", submissionLatencyMs);
      PerformanceMetrics.announce(job.getId(), responsePayload);
      job.markQueued(responsePayload);
    } catch (RestClientResponseException exception) {
      job.fail(exception.getMessage());
      generationJobRepository.save(job);
      throw exception;
    }

    return generationJobRepository.save(job);
  }

  /**
   * 이미지 개수(0/1/2장 이상)와 highQuality 플래그로 fal.ai 모델을 자동 분기 선택하고, refinePrompt가
   * true(또는 미지정)이면 Claude Sonnet 물리 보정을 선행 실행해 그 결과를 최종 prompt로 주입한 뒤 비동기
   * 큐에 제출한다(api_2.md 분기 엔진 규격). refinePrompt=false면 사용자가 입력한 원본 prompt를 그대로 쓴다.
   */
  public GenerationJob generateVideo(
      Long userId,
      Long chatId,
      boolean highQuality,
      List<String> images,
      String prompt,
      Integer duration,
      String aspectRatio,
      Boolean refinePrompt) {
    List<String> safeImages = images == null ? List.of() : images;
    if (safeImages.size() > MAX_REFERENCE_IMAGES) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT, List.of("참조 이미지는 최대 " + MAX_REFERENCE_IMAGES + "장까지만 지원합니다."));
    }
    boolean shouldRefine = refinePrompt == null || refinePrompt;

    String modelCode = resolveVideoModelCode(highQuality, safeImages.size());

    Map<String, Object> requestPayload = new LinkedHashMap<>();
    requestPayload.put("modelCode", modelCode);
    requestPayload.put("highQuality", highQuality);
    requestPayload.put("images", safeImages);
    requestPayload.put("duration", duration);
    requestPayload.put("aspectRatio", aspectRatio);
    requestPayload.put("refinePromptEnabled", shouldRefine);

    GenerationJob job =
        generationJobRepository.save(
            GenerationJob.create(
                userId, chatId, null, null, GenerationType.VIDEO_GENERATION.name(), prompt, requestPayload));

    try {
      Map<String, Object> responsePayload = new LinkedHashMap<>();
      String finalPrompt = prompt;

      if (shouldRefine) {
        long refineStartedAtMs = System.currentTimeMillis();
        byte[] referenceImageBytes = safeImages.isEmpty() ? null : tryDecodeImage(safeImages.get(0));
        AiTextGenerationResult refined =
            promptGenerationPort.generateFromImage(referenceImageBytes, "image/png", prompt);
        long refineDurationMs = System.currentTimeMillis() - refineStartedAtMs;

        finalPrompt = refined.text();
        responsePayload.put("refinedPrompt", finalPrompt);
        PerformanceMetrics.record(responsePayload, "refineDurationMs", refineDurationMs);
      }

      Map<String, Object> input = new LinkedHashMap<>();
      input.put("prompt", finalPrompt);
      if (duration != null) {
        input.put("duration", duration);
      }
      if (aspectRatio != null) {
        input.put("aspect_ratio", aspectRatio);
      }
      if (!safeImages.isEmpty()) {
        input.put("images", safeImages);
      }

      long submitStartedAtMs = System.currentTimeMillis();
      FalGenerationSubmission submission = falGenerationPort.submit(modelCode, input);
      long submissionLatencyMs = System.currentTimeMillis() - submitStartedAtMs;

      responsePayload.put("requestId", submission.externalRequestId());
      responsePayload.put("statusUrl", submission.statusUrl());
      responsePayload.put("responseUrl", submission.responseUrl());
      responsePayload.put("raw", submission.rawResponse());
      PerformanceMetrics.record(responsePayload, "submissionLatencyMs", submissionLatencyMs);
      PerformanceMetrics.announce(job.getId(), responsePayload);
      job.markQueued(responsePayload);
    } catch (RestClientResponseException exception) {
      job.fail(exception.getMessage());
      generationJobRepository.save(job);
      throw exception;
    }

    return generationJobRepository.save(job);
  }

  public GenerationJob getStatus(Long jobId) {
    GenerationJob job = findJob(jobId);
    if (isTerminal(job.getStatus())) {
      return job;
    }

    Map<String, Object> requestPayload = job.getRequestPayload();
    Map<String, Object> responsePayload = job.getResponsePayload();
    if (requestPayload == null || responsePayload == null) {
      return job;
    }

    Object modelCodeValue = requestPayload.get("modelCode");
    Object externalRequestIdValue = responsePayload.get("requestId");
    if (!(modelCodeValue instanceof String modelCode) || !(externalRequestIdValue instanceof String externalRequestId)) {
      return job;
    }

    FalGenerationStatus polled = falGenerationPort.poll(modelCode, externalRequestId);
    GenerationStatus newStatus = GenerationStatus.fromFalStatus(polled.status());

    Map<String, Object> merged = new LinkedHashMap<>(responsePayload);
    merged.putAll(polled.rawResponse());

    newStatus = generatedMediaUploader.applyCompletion(job, newStatus, merged);

    job.applyPolledStatus(newStatus, merged);
    return generationJobRepository.save(job);
  }

  private GenerationJob findJob(Long jobId) {
    return generationJobRepository
        .findById(jobId)
        .orElseThrow(() -> new BusinessException(ErrorCode.GENERATION_NOT_FOUND));
  }

  private GenerationType parseGenerationType(String jobType) {
    try {
      return GenerationType.valueOf(jobType);
    } catch (IllegalArgumentException exception) {
      throw new BusinessException(ErrorCode.INVALID_INPUT);
    }
  }

  private Map<String, Object> toResponsePayload(AiTextGenerationResult result) {
    Map<String, Object> responsePayload = new LinkedHashMap<>();
    responsePayload.put("text", result.text());
    responsePayload.put("raw", result.rawResponse());
    return responsePayload;
  }

  private byte[] decodeImage(String imageBase64) {
    try {
      return Base64.getDecoder().decode(imageBase64);
    } catch (IllegalArgumentException exception) {
      throw new BusinessException(ErrorCode.INVALID_INPUT);
    }
  }

  /**
   * highQuality=true -> ByteDance Seedance 2.0, false -> fal.ai Kling O3 Standard.
   * 이미지 0장은 text-to-video, 1장은 image-to-video, 2장 이상은 reference-to-video로 분기한다.
   */
  private String resolveVideoModelCode(boolean highQuality, int imageCount) {
    String family = highQuality ? "bytedance/seedance-2.0" : "fal-ai/kling-video/o3/standard";
    String variant =
        imageCount == 0 ? "text-to-video" : imageCount == 1 ? "image-to-video" : "reference-to-video";
    return family + "/" + variant;
  }

  /**
   * Claude 보정 선행 호출에 참고 이미지로 쓸 바이트를 시도해 디코딩한다. images 원소는 Base64/데이터 URL일
   * 수도, S3 영구 URL일 수도 있다(api_2.md 2번 규격) — S3 URL은 다운로드하지 않고 텍스트만으로 보정한다.
   */
  private byte[] tryDecodeImage(String imageValue) {
    if (imageValue == null || imageValue.isBlank() || imageValue.startsWith("http://")
        || imageValue.startsWith("https://")) {
      return null;
    }
    try {
      String base64Part = imageValue.contains(",") ? imageValue.substring(imageValue.indexOf(',') + 1) : imageValue;
      return Base64.getDecoder().decode(base64Part);
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  private boolean isTerminal(String status) {
    return GenerationStatus.COMPLETED.name().equals(status)
        || GenerationStatus.FAILED.name().equals(status)
        || GenerationStatus.CANCELED.name().equals(status)
        || GenerationStatus.EXPIRED.name().equals(status);
  }
}
