package com.likelion.a1.generation.application.service;

import com.likelion.a1.generation.domain.model.GenerationJob;
import com.likelion.a1.generation.domain.model.GenerationStatus;
import com.likelion.a1.generation.domain.model.GenerationType;
import com.likelion.a1.generation.domain.repository.GenerationJobRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * QUEUED/PROCESSING 상태인 IMAGE_GENERATION/VIDEO_GENERATION GenerationJob을 5초 주기로 fal.ai에 재폴링하고,
 * COMPLETED 판정 시 {@link GeneratedMediaUploader}를 통해 fal.ai의 임시 미디어 URL을 S3로 영구 이관한 뒤
 * s3Url을 responsePayload에 채운다. 이 완료 처리 로직은 {@link GenerationAiService#getStatus}와 공유된다 —
 * 사용자가 수동으로 상태를 먼저 조회해도 S3 업로드가 스킵되지 않도록 하기 위함이다.
 *
 * <p>이 클래스 자체는 트랜잭션을 걸지 않는다 — 실제 폴링/완료 처리는 {@link GenerationJobPoller}가 job마다
 * 독립된 트랜잭션(REQUIRES_NEW)으로 수행하므로, 한 job의 @Version 충돌이나 예외가 같은 사이클의 다른
 * job 처리 결과를 롤백시키지 않는다.
 */
@Component
public class GenerationVideoPollingScheduler {
  private static final Logger log = LoggerFactory.getLogger(GenerationVideoPollingScheduler.class);
  private static final List<String> POLLABLE_STATUSES =
      List.of(GenerationStatus.QUEUED.name(), GenerationStatus.PROCESSING.name());
  private static final List<String> POLLABLE_TYPES =
      List.of(GenerationType.IMAGE_GENERATION.name(), GenerationType.VIDEO_GENERATION.name());

  private final GenerationJobRepository generationJobRepository;
  private final GenerationJobPoller generationJobPoller;

  public GenerationVideoPollingScheduler(
      GenerationJobRepository generationJobRepository, GenerationJobPoller generationJobPoller) {
    this.generationJobRepository = generationJobRepository;
    this.generationJobPoller = generationJobPoller;
  }

  @Scheduled(fixedDelay = 5000)
  public void pollPendingJobs() {
    for (GenerationJob job : generationJobRepository.findByStatusIn(POLLABLE_STATUSES)) {
      if (POLLABLE_TYPES.contains(job.getGenerationType())) {
        try {
          generationJobPoller.pollJob(job.getId());
        } catch (RuntimeException exception) {
          // 여기는 트랜잭션 경계 밖이라 이 catch가 다른 job 처리에 영향을 주지 않는다. @Version
          // 충돌이면 다른 폴러가 이미 완료 처리했다는 뜻이라 다음 사이클에서 자연히 스킵되고,
          // 그 외 예외라도 이 job만 이번 사이클에 건너뛰고 다음 5초 주기에 다시 시도한다.
          log.warn("Job {} 폴링 처리 실패 — 이번 사이클은 스킵합니다.", job.getId(), exception);
        }
      }
    }
  }
}
