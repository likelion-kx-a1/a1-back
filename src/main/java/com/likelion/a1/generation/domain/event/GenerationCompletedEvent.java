package com.likelion.a1.generation.domain.event;

import com.likelion.a1.generation.domain.model.GenerationJob;
import java.util.Map;

/** GenerationJob이 COMPLETED/FAILED로 종결됐을 때 발행되는 도메인 이벤트. SSE로 결과를 push하는 데 쓰인다. */
public record GenerationCompletedEvent(
    Long userId, Long jobId, String jobType, String status, String resultUrl) {

  public static GenerationCompletedEvent from(GenerationJob job) {
    return new GenerationCompletedEvent(
        job.getUserId(),
        job.getId(),
        job.getGenerationType(),
        job.getStatus(),
        extractResultUrl(job.getResponsePayload()));
  }

  private static String extractResultUrl(Map<String, Object> responsePayload) {
    if (responsePayload == null) {
      return null;
    }
    return responsePayload.get("s3Url") instanceof String url ? url : null;
  }
}
