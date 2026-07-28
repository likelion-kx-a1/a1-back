package com.likelion.a1.generation.application.service;

import com.likelion.a1.generation.application.port.out.FalGenerationPort;
import com.likelion.a1.generation.application.port.out.FalGenerationStatus;
import com.likelion.a1.generation.domain.model.GenerationJob;
import com.likelion.a1.generation.domain.model.GenerationStatus;
import com.likelion.a1.generation.domain.repository.GenerationJobRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

/**
 * 개별 GenerationJob 하나를 자기 자신만의 독립 트랜잭션(REQUIRES_NEW)으로 폴링/완료 처리한다.
 * {@link GenerationVideoPollingScheduler}가 한 사이클에 여러 job을 순회할 때, 이 트랜잭션을 job마다
 * 분리해 두지 않으면 job A에서 @Version 낙관적 락 충돌이 나는 순간 그 트랜잭션 전체가 rollback-only로
 * 표시되어(Hibernate 특성) 같은 사이클에서 이미 정상 처리된 job B/C의 커밋까지 함께 날아간다.
 */
@Component
class GenerationJobPoller {
  private final GenerationJobRepository generationJobRepository;
  private final FalGenerationPort falGenerationPort;
  private final GeneratedMediaUploader generatedMediaUploader;

  GenerationJobPoller(
      GenerationJobRepository generationJobRepository,
      FalGenerationPort falGenerationPort,
      GeneratedMediaUploader generatedMediaUploader) {
    this.generationJobRepository = generationJobRepository;
    this.falGenerationPort = falGenerationPort;
    this.generatedMediaUploader = generatedMediaUploader;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void pollJob(Long jobId) {
    // 수동 상태 조회와 백그라운드 스케줄러가 같은 job을 동시에 완료 처리하지 못하도록
    // 트랜잭션이 끝날 때까지 DB 행 잠금을 보유한다.
    GenerationJob job = generationJobRepository.findByIdForUpdate(jobId).orElse(null);
    if (job == null) {
      return;
    }

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
    String statusUrl = stringValue(responsePayload.get("statusUrl"));
    String responseUrl = stringValue(responsePayload.get("responseUrl"));

    FalGenerationStatus polled;
    try {
      polled = falGenerationPort.poll(modelCode, externalRequestId, statusUrl, responseUrl);
    } catch (RestClientException exception) {
      return;
    }

    GenerationStatus newStatus = GenerationStatus.fromFalStatus(polled.status());
    Map<String, Object> merged = new LinkedHashMap<>(responsePayload);
    merged.putAll(polled.rawResponse());

    newStatus = generatedMediaUploader.applyCompletion(job, newStatus, merged);
    if (newStatus == null) {
      // 다른 폴러가 이미 이 job을 종결 처리했다 — 저장을 시도하면 100% 낙관적 락 충돌만 나므로
      // 아예 시도하지 않고 그대로 반환한다(docs_h/제노바_KX_생성결과.md 참고).
      return;
    }

    job.applyPolledStatus(newStatus, merged, errorMessage(polled));
    // 그래도 남는 좁은 race window(위 체크 이후 저장 사이)에서 충돌이 나면 이 메서드 밖
    // (REQUIRES_NEW 트랜잭션 경계)으로 그대로 던진다 — 이 job의 독립 트랜잭션만 깨끗하게 롤백되고,
    // 호출자(GenerationVideoPollingScheduler)가 job 단위로 감싼 try/catch에서 로그만 남기고 다음
    // job으로 넘어간다. 다른 폴러가 이미 완료 처리했다는 뜻이므로 실질적 데이터 유실은 없다.
    generationJobRepository.save(job);
  }

  private String stringValue(Object value) {
    return value instanceof String string && !string.isBlank() ? string : null;
  }

  private String errorMessage(FalGenerationStatus status) {
    return stringValue(status.rawResponse().get("errorMessage"));
  }
}
