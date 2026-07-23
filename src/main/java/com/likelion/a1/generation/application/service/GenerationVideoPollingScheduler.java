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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
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
  private static final Logger log = LoggerFactory.getLogger(GenerationVideoPollingScheduler.class);
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
    try {
      generationJobRepository.save(job);
    } catch (OptimisticLockingFailureException exception) {
      // 수동 polling(getStatus)이 같은 job을 먼저 커밋한 경우. 이 job만 스킵하고 배치의 나머지 job은
      // 계속 처리한다 — 여기서 예외를 밖으로 새게 두면 pollPendingJobs() 트랜잭션 전체가 롤백되어
      // 이미 정상 처리된 다른 job들까지 되돌아간다.
      log.info("Job {} 동시 완료 처리 충돌 감지 — 다른 폴러가 이미 저장함, 스킵합니다.", job.getId());
    }
  }
}
