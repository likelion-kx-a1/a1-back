package com.likelion.a1.generation.infrastructure.client.openai;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.likelion.a1.generation.application.port.out.AiTextGenerationResult;
import com.likelion.a1.generation.application.port.out.ImageAnalysisPort;
import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import java.util.Base64;
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

/** prod 프로필용 실제 OpenAI Chat Completions(Vision) API 연동. 이미지의 구도/질감/무중력 요소를 역프롬프트로 추출한다. */
@Component
@Profile("prod")
public class GptVisionImageAnalysisAdapter implements ImageAnalysisPort {
  private final RestTemplate restTemplate = new RestTemplate();
  private final ObjectMapper objectMapper;
  private final String baseUrl;
  private final String apiKey;
  private final String model;

  public GptVisionImageAnalysisAdapter(
      ObjectMapper objectMapper,
      @Value("${app.ai.openai.base-url}") String baseUrl,
      @Value("${app.ai.openai.api-key}") String apiKey,
      @Value("${app.ai.openai.model}") String model) {
    this.objectMapper = objectMapper;
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.model = model;
  }

  @Override
  public AiTextGenerationResult analyze(byte[] imageBytes, String mimeType, String instruction) {
    ResponseEntity<String> response =
        restTemplate.exchange(
            baseUrl + "/v1/chat/completions",
            HttpMethod.POST,
            new HttpEntity<>(buildRequestBody(imageBytes, mimeType, instruction), buildHeaders()),
            String.class);

    Map<String, Object> raw = parseJson(response.getBody());
    return new AiTextGenerationResult(extractContent(raw), raw);
  }

  private Map<String, Object> buildRequestBody(byte[] imageBytes, String mimeType, String instruction) {
    String dataUrl = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(imageBytes);

    Map<String, Object> textContent = new LinkedHashMap<>();
    textContent.put("type", "text");
    textContent.put("text", instruction);

    Map<String, Object> imageUrl = new LinkedHashMap<>();
    imageUrl.put("url", dataUrl);

    Map<String, Object> imageContent = new LinkedHashMap<>();
    imageContent.put("type", "image_url");
    imageContent.put("image_url", imageUrl);

    Map<String, Object> message = new LinkedHashMap<>();
    message.put("role", "user");
    message.put("content", List.of(textContent, imageContent));

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("model", model);
    body.put("messages", List.of(message));
    return body;
  }

  private HttpHeaders buildHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(apiKey);
    return headers;
  }

  @SuppressWarnings("unchecked")
  private String extractContent(Map<String, Object> raw) {
    Object choicesObj = raw.get("choices");
    if (choicesObj instanceof List<?> choices && !choices.isEmpty()
        && choices.get(0) instanceof Map<?, ?> firstChoice
        && firstChoice.get("message") instanceof Map<?, ?> messageMap
        && messageMap.get("content") instanceof String content) {
      return content;
    }
    throw new BusinessException(ErrorCode.AI_PROVIDER_REQUEST_FAILED);
  }

  private Map<String, Object> parseJson(String body) {
    try {
      return objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
    } catch (RuntimeException exception) {
      throw new BusinessException(ErrorCode.AI_PROVIDER_REQUEST_FAILED);
    }
  }
}
