package com.likelion.a1.generation.application.service;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GenerationJob.responsePayload의 "performance" 서브맵에 계측값을 안전하게 병합하고, 콘솔에
 * 사람이 읽기 좋은 형태로 출력한다. {@link GenerationAiService}(동기 호출/제출 지연)와
 * {@link GeneratedMediaUploader}(비동기 완공 총 시간)가 이 유틸을 공유한다.
 */
final class PerformanceMetrics {
  private static final Logger log = LoggerFactory.getLogger(PerformanceMetrics.class);

  private PerformanceMetrics() {}

  /** payload["performance"][key] = value로 병합한다. 기존에 쌓인 다른 지표는 보존된다. */
  @SuppressWarnings("unchecked")
  static void record(Map<String, Object> payload, String key, Object value) {
    Object existing = payload.get("performance");
    Map<String, Object> performance =
        existing instanceof Map<?, ?> map
            ? new LinkedHashMap<>((Map<String, Object>) map)
            : new LinkedHashMap<>();
    performance.put(key, value);
    payload.put("performance", performance);
  }

  /** payload["performance"]에 쌓인 지표를 🪐 [성능 지표] 한 줄 로그로 출력한다. */
  static void announce(Long jobId, Map<String, Object> payload) {
    Object raw = payload.get("performance");
    if (!(raw instanceof Map<?, ?> performance) || performance.isEmpty()) {
      return;
    }

    StringBuilder line = new StringBuilder("🪐 [성능 지표] Job ID: ").append(jobId);
    appendIfPresent(line, performance, "refineDurationMs", "Claude 보정", "ms");
    appendIfPresent(line, performance, "analysisDurationMs", "GPT Vision 역분석", "ms");
    appendIfPresent(line, performance, "submissionLatencyMs", "fal.ai 제출 지연", "ms");
    appendIfPresent(line, performance, "asyncPollingDurationSec", "총 비동기 완공 시간", "초");
    log.info(line.toString());
  }

  private static void appendIfPresent(
      StringBuilder line, Map<?, ?> performance, String key, String label, String unit) {
    if (performance.get(key) instanceof Number number) {
      line.append(" | ").append(label).append(": ").append(String.format("%,d", number.longValue())).append(unit);
    }
  }
}
