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
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;

/** 프롬프트 보정, 역프롬프트 추출, fal.ai 비동기 제출/폴링 전체 파이프라인을 조율하고 GenerationJob으로 영속화한다. */
@Service
@Transactional
public class GenerationAiService {
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

  private boolean isTerminal(String status) {
    return GenerationStatus.COMPLETED.name().equals(status)
        || GenerationStatus.FAILED.name().equals(status)
        || GenerationStatus.CANCELED.name().equals(status)
        || GenerationStatus.EXPIRED.name().equals(status);
  }
}
