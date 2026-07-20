package com.likelion.a1.generation.application.service;

import com.likelion.a1.generation.application.port.out.FalGenerationPort;
import com.likelion.a1.generation.application.port.out.FalGenerationStatus;
import com.likelion.a1.generation.domain.model.GenerationJob;
import com.likelion.a1.generation.domain.model.GenerationStatus;
import com.likelion.a1.generation.domain.model.GenerationType;
import com.likelion.a1.generation.domain.repository.GenerationJobRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

/**
 * QUEUED/PROCESSING 상태인 IMAGE_GENERATION/VIDEO_GENERATION GenerationJob을 5초 주기로 fal.ai에 재폴링하고,
 * COMPLETED 판정 시 {@link GeneratedMediaUploader}를 통해 fal.ai의 임시 미디어 URL을 S3로 영구 이관한 뒤
 * s3Url을 responsePayload에 채운다. 이 완료 처리 로직은 {@link GenerationAiService#getStatus}와 공유된다 —
 * 사용자가 수동으로 상태를 먼저 조회해도 S3 업로드가 스킵되지 않도록 하기 위함이다.
 */
@Component
public class GenerationVideoPollingScheduler {
  private static final List<String> POLLABLE_STATUSES =
      List.of(GenerationStatus.QUEUED.name(), GenerationStatus.PROCESSING.name());
  private static final List<String> POLLABLE_TYPES =
      List.of(GenerationType.IMAGE_GENERATION.name(), GenerationType.VIDEO_GENERATION.name());

  private final GenerationJobRepository generationJobRepository;
  private final FalGenerationPort falGenerationPort;
  private final GeneratedMediaUploader generatedMediaUploader;

  public GenerationVideoPollingScheduler(
      GenerationJobRepository generationJobRepository,
      FalGenerationPort falGenerationPort,
      GeneratedMediaUploader generatedMediaUploader) {
    this.generationJobRepository = generationJobRepository;
    this.falGenerationPort = falGenerationPort;
    this.generatedMediaUploader = generatedMediaUploader;
  }

  @Scheduled(fixedDelay = 5000)
  @Transactional
  public void pollPendingJobs() {
    for (GenerationJob job : generationJobRepository.findByStatusIn(POLLABLE_STATUSES)) {
      if (POLLABLE_TYPES.contains(job.getGenerationType())) {
        pollJob(job);
      }
    }
  }

  private void pollJob(GenerationJob job) {
    Map<String, Object> requestPayload = job.getRequestPayload();
    Map<String, Object> responsePayload = job.getResponsePayload();
    if (requestPayload == null || responsePayload == null) {
      return;
    }

    Object modelCodeValue = requestPayload.get("modelCode");
    Object externalRequestIdValue = responsePayload.get("requestId");
    if (!(modelCodeValue instanceof String modelCode) || !(externalRequestIdValue instanceof String externalRequestId)) {
      return;
    }

    FalGenerationStatus polled;
    try {
      polled = falGenerationPort.poll(modelCode, externalRequestId);
    } catch (RestClientException exception) {
      return;
    }

    GenerationStatus newStatus = GenerationStatus.fromFalStatus(polled.status());
    Map<String, Object> merged = new LinkedHashMap<>(responsePayload);
    merged.putAll(polled.rawResponse());

    newStatus = generatedMediaUploader.applyCompletion(job, newStatus, merged);

    job.applyPolledStatus(newStatus, merged);
    generationJobRepository.save(job);
  }
}
