package com.likelion.a1.generation.infrastructure.client.fal;

import com.likelion.a1.generation.application.port.out.FalGenerationPort;
import com.likelion.a1.generation.application.port.out.FalGenerationStatus;
import com.likelion.a1.generation.application.port.out.FalGenerationSubmission;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * local/default 프로필용 fal.ai 큐 에뮬레이터. 실제 과금 호출 없이 mock-request-UUID를 발급하고,
 * 같은 externalRequestId로 두 번째 폴링부터 COMPLETED와 샘플 미디어 주소(data URI, 네트워크 호출 없음)를 반환한다.
 */
@Component
@Profile({"local", "default"})
public class MockFalGenerationAdapter implements FalGenerationPort {
  private static final String MOCK_MEDIA_BASE64 =
      "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUAAarVyFEAAAAASUVORK5CYII=";

  private final Map<String, String> modelCodeByRequestId = new ConcurrentHashMap<>();
  private final Map<String, Integer> pollCountByRequestId = new ConcurrentHashMap<>();

  @Override
  public FalGenerationSubmission submit(String modelCode, Map<String, Object> input) {
    String requestId = "mock-request-" + UUID.randomUUID();
    modelCodeByRequestId.put(requestId, modelCode);
    pollCountByRequestId.put(requestId, 0);

    Map<String, Object> raw = new LinkedHashMap<>();
    raw.put("mock", true);
    raw.put("request_id", requestId);
    raw.put("status", "IN_QUEUE");
    raw.put("modelCode", modelCode);
    raw.put("input", input);

    String statusUrl = "https://mock.fal.run/requests/" + requestId + "/status";
    String responseUrl = "https://mock.fal.run/requests/" + requestId;
    return new FalGenerationSubmission(requestId, statusUrl, responseUrl, raw);
  }

  @Override
  public FalGenerationStatus poll(String modelCode, String externalRequestId) {
    int attempt = pollCountByRequestId.merge(externalRequestId, 1, Integer::sum);

    Map<String, Object> raw = new LinkedHashMap<>();
    raw.put("mock", true);
    raw.put("request_id", externalRequestId);

    if (attempt < 2) {
      raw.put("status", "IN_PROGRESS");
      return new FalGenerationStatus("IN_PROGRESS", raw);
    }

    raw.put("status", "COMPLETED");
    String resolvedModelCode = modelCodeByRequestId.getOrDefault(externalRequestId, modelCode);
    if (isVideoModel(resolvedModelCode)) {
      Map<String, Object> video = new LinkedHashMap<>();
      video.put("url", "data:video/mp4;base64," + MOCK_MEDIA_BASE64);
      video.put("engine", isHighQuality(resolvedModelCode) ? "seedance-2.0" : "kling-o3-standard");
      video.put("variant", resolveVideoVariant(resolvedModelCode));
      raw.put("video", video);
    } else {
      Map<String, Object> image = new LinkedHashMap<>();
      image.put("url", "data:image/png;base64," + MOCK_MEDIA_BASE64);
      raw.put("images", List.of(image));
    }

    return new FalGenerationStatus("COMPLETED", raw);
  }

  private boolean isVideoModel(String modelCode) {
    return modelCode != null && modelCode.toLowerCase().contains("video");
  }

  /** api_2.md 분기 엔진 규격: seedance-2.0(고품질) vs kling(빠른 생성) 모델 패밀리 판정. */
  private boolean isHighQuality(String modelCode) {
    return modelCode != null && modelCode.toLowerCase().contains("seedance");
  }

  /** modelCode 슬러그 말단(text/image/reference-to-video)으로 어떤 이미지 개수 분기였는지 되짚는다. */
  private String resolveVideoVariant(String modelCode) {
    if (modelCode == null) {
      return "text-to-video";
    }
    String lower = modelCode.toLowerCase();
    if (lower.contains("reference-to-video")) {
      return "reference-to-video";
    }
    if (lower.contains("image-to-video")) {
      return "image-to-video";
    }
    return "text-to-video";
  }
}
