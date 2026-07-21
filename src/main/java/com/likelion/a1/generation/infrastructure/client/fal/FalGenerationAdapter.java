package com.likelion.a1.generation.infrastructure.client.fal;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.likelion.a1.generation.application.port.out.FalGenerationPort;
import com.likelion.a1.generation.application.port.out.FalGenerationStatus;
import com.likelion.a1.generation.application.port.out.FalGenerationSubmission;
import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/** prod 프로필용 실제 fal.ai 큐 API 연동. Kling/Seedance/GPT Image 2 등 모델 코드를 그대로 라우팅한다. */
@Component
@Profile("prod")
public class FalGenerationAdapter implements FalGenerationPort {
  private final RestTemplate restTemplate = new RestTemplate();
  private final ObjectMapper objectMapper;
  private final String baseUrl;
  private final String apiKey;

  public FalGenerationAdapter(
      ObjectMapper objectMapper,
      @Value("${app.ai.fal.base-url}") String baseUrl,
      @Value("${app.ai.fal.api-key}") String apiKey) {
    this.objectMapper = objectMapper;
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
  }

  @Override
  public FalGenerationSubmission submit(String modelCode, Map<String, Object> input) {
    ResponseEntity<String> response =
        restTemplate.exchange(
            baseUrl + "/" + modelCode,
            HttpMethod.POST,
            new HttpEntity<>(mapImagesForFalPayload(input), buildHeaders()),
            String.class);

    Map<String, Object> raw = parseJson(response.getBody());
    String requestId = String.valueOf(raw.get("request_id"));
    String statusUrl = resolveUrl(raw.get("status_url"), modelCode, requestId, "/status");
    String responseUrl = resolveUrl(raw.get("response_url"), modelCode, requestId, "");
    return new FalGenerationSubmission(requestId, statusUrl, responseUrl, raw);
  }

  @Override
  public FalGenerationStatus poll(String modelCode, String externalRequestId) {
    String statusUrl = baseUrl + "/" + modelCode + "/requests/" + externalRequestId + "/status";
    ResponseEntity<String> statusResponse =
        restTemplate.exchange(statusUrl, HttpMethod.GET, new HttpEntity<>(buildHeaders()), String.class);

    Map<String, Object> raw = new LinkedHashMap<>(parseJson(statusResponse.getBody()));
    String status = String.valueOf(raw.get("status"));

    if ("COMPLETED".equals(status)) {
      String responseUrl = baseUrl + "/" + modelCode + "/requests/" + externalRequestId;
      ResponseEntity<String> resultResponse =
          restTemplate.exchange(responseUrl, HttpMethod.GET, new HttpEntity<>(buildHeaders()), String.class);
      raw.putAll(parseJson(resultResponse.getBody()));
    }

    return new FalGenerationStatus(status, raw);
  }

  /**
   * GenerationAiService는 프로토콜에 무관한 공용 "images" 키만 채운다. fal.ai 각 모델의 실제 스펙에 맞춰
   * 여기서 최종 매핑한다: 1장이면 image_url(단일 문자열), 2장 이상이면 reference_images(배열)로 변환한다
   * (api_2.md 3번 규격). 0장(text-to-video)이면 images 키 자체가 없으므로 원본 그대로 전달한다.
   */
  private Map<String, Object> mapImagesForFalPayload(Map<String, Object> input) {
    if (!(input.get("images") instanceof List<?> images) || images.isEmpty()) {
      return input;
    }

    Map<String, Object> mapped = new LinkedHashMap<>(input);
    mapped.remove("images");
    if (images.size() == 1) {
      mapped.put("image_url", images.get(0));
    } else {
      mapped.put("reference_images", images);
    }
    return mapped;
  }

  private String resolveUrl(Object candidate, String modelCode, String requestId, String suffix) {
    if (candidate instanceof String url && !url.isBlank()) {
      return url;
    }
    return baseUrl + "/" + modelCode + "/requests/" + requestId + suffix;
  }

  private HttpHeaders buildHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Key " + apiKey);
    return headers;
  }

  private Map<String, Object> parseJson(String body) {
    try {
      return objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
    } catch (RuntimeException exception) {
      throw new BusinessException(ErrorCode.AI_PROVIDER_REQUEST_FAILED);
    }
  }
}
