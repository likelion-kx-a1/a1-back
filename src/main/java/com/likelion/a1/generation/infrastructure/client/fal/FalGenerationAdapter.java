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
import org.springframework.web.client.RestClientResponseException;
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
            new HttpEntity<>(mapImagesForFalPayload(modelCode, input), buildHeaders()),
            String.class);

    Map<String, Object> raw = parseJson(response.getBody());
    String requestId = String.valueOf(raw.get("request_id"));
    String statusUrl = resolveUrl(raw.get("status_url"), modelCode, requestId, "/status");
    String responseUrl = resolveUrl(raw.get("response_url"), modelCode, requestId, "");
    return new FalGenerationSubmission(requestId, statusUrl, responseUrl, raw);
  }

  @Override
  public FalGenerationStatus poll(
      String modelCode,
      String externalRequestId,
      String statusUrl,
      String responseUrl) {
    String resolvedStatusUrl = resolveUrl(statusUrl, modelCode, externalRequestId, "/status");
    ResponseEntity<String> statusResponse =
        restTemplate.exchange(
            resolvedStatusUrl,
            HttpMethod.GET,
            new HttpEntity<>(buildHeaders()),
            String.class);

    Map<String, Object> raw = new LinkedHashMap<>(parseJson(statusResponse.getBody()));
    String status = String.valueOf(raw.get("status"));

    if ("COMPLETED".equals(status)) {
      String resolvedResponseUrl = resolveUrl(responseUrl, modelCode, externalRequestId, "");
      try {
        ResponseEntity<String> resultResponse =
            restTemplate.exchange(
                resolvedResponseUrl,
                HttpMethod.GET,
                new HttpEntity<>(buildHeaders()),
                String.class);
        raw.putAll(parseJson(resultResponse.getBody()));
      } catch (RestClientResponseException exception) {
        raw.put("status", "FAILED");
        raw.put("providerHttpStatus", exception.getStatusCode().value());
        raw.put("providerError", parseErrorBody(exception.getResponseBodyAsString()));
        raw.put("errorMessage", resolveErrorMessage(exception));
        status = "FAILED";
      }
    }

    return new FalGenerationStatus(status, raw);
  }

  /**
   * GenerationAiService는 프로토콜에 무관한 공용 "images" 키만 채운다. fal.ai 각 모델의 실제 스펙에 맞춰
   * 여기서 최종 매핑한다. GPT Image 2 편집은 image_urls 배열을 사용하고, 영상 계열은 1장이면
   * image_url, 2장 이상이면 reference_images로 변환한다. 이미지가 없으면 텍스트 생성 요청으로 전달한다.
   */
  private Map<String, Object> mapImagesForFalPayload(
      String modelCode, Map<String, Object> input) {
    Map<String, Object> mapped = mapImageSizeForGptImage(modelCode, input);

    if (!(input.get("images") instanceof List<?> images) || images.isEmpty()) {
      return mapped;
    }

    mapped.remove("images");
    if ("openai/gpt-image-2/edit".equals(modelCode)) {
      mapped.put("image_urls", images);
      return mapped;
    }

    if (images.size() == 1) {
      mapped.put("image_url", images.get(0));
    } else {
      mapped.put("reference_images", images);
    }
    return mapped;
  }

  private Map<String, Object> mapImageSizeForGptImage(
      String modelCode, Map<String, Object> input) {
    if (!modelCode.startsWith("openai/gpt-image-2")) {
      return input;
    }

    Map<String, Object> mapped = new LinkedHashMap<>(input);
    Object aspectRatio = mapped.remove("aspect_ratio");
    if (aspectRatio instanceof String ratio && !mapped.containsKey("image_size")) {
      mapped.put(
          "image_size",
          switch (ratio) {
            case "16:9" -> "landscape_16_9";
            case "9:16" -> "portrait_16_9";
            default -> "square_hd";
          });
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

  private Object parseErrorBody(String body) {
    try {
      return objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
    } catch (RuntimeException exception) {
      return body;
    }
  }

  private String resolveErrorMessage(RestClientResponseException exception) {
    Map<String, Object> error = parseJsonSafely(exception.getResponseBodyAsString());
    Object detail = error.get("detail");
    if (detail instanceof List<?> details
        && !details.isEmpty()
        && details.get(0) instanceof Map<?, ?> first
        && first.get("msg") instanceof String message
        && !message.isBlank()) {
      return message;
    }
    return "FAL result request failed with HTTP " + exception.getStatusCode().value();
  }

  private Map<String, Object> parseJsonSafely(String body) {
    try {
      return objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
    } catch (RuntimeException exception) {
      return Map.of();
    }
  }
}
