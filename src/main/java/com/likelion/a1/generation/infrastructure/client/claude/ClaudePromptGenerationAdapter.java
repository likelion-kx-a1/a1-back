package com.likelion.a1.generation.infrastructure.client.claude;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.likelion.a1.generation.application.port.out.AiTextGenerationResult;
import com.likelion.a1.generation.application.port.out.PromptGenerationPort;
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

/** prod 프로필용 실제 Anthropic Messages API 연동. 참고 이미지와 한국어 지시문을 무중력 시네마틱 영문 프롬프트로 보정한다. */
@Component
@Profile("prod")
public class ClaudePromptGenerationAdapter implements PromptGenerationPort {
  private static final String PROMPT_SYSTEM_INSTRUCTION =
      "You are a cinematic prompt engineer specialized in anti-gravity, zero-gravity physics-based "
          + "visual generation. Expand the following Korean instruction into a single, richly detailed "
          + "English prompt describing weightless, zero-gravity cinematic physics (suspended liquid, "
          + "inertia decay, volumetric particles, orbiting camera movement). Respond with the English "
          + "prompt text only, no preamble.\n\n지시문: ";

  private final RestTemplate restTemplate = new RestTemplate();
  private final ObjectMapper objectMapper;
  private final String baseUrl;
  private final String apiKey;
  private final String model;
  private final String anthropicVersion;

  public ClaudePromptGenerationAdapter(
      ObjectMapper objectMapper,
      @Value("${app.ai.claude.base-url}") String baseUrl,
      @Value("${app.ai.claude.api-key}") String apiKey,
      @Value("${app.ai.claude.model}") String model,
      @Value("${app.ai.claude.version}") String anthropicVersion) {
    this.objectMapper = objectMapper;
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.model = model;
    this.anthropicVersion = anthropicVersion;
  }

  @Override
  public AiTextGenerationResult generateFromImage(byte[] imageBytes, String mimeType, String instruction) {
    ResponseEntity<String> response =
        restTemplate.exchange(
            baseUrl + "/v1/messages",
            HttpMethod.POST,
            new HttpEntity<>(buildRequestBody(imageBytes, mimeType, instruction), buildHeaders()),
            String.class);

    Map<String, Object> raw = parseJson(response.getBody());
    return new AiTextGenerationResult(extractText(raw), raw);
  }

  private Map<String, Object> buildRequestBody(byte[] imageBytes, String mimeType, String instruction) {
    Map<String, Object> textContent = new LinkedHashMap<>();
    textContent.put("type", "text");
    textContent.put("text", PROMPT_SYSTEM_INSTRUCTION + instruction);

    List<Object> content;
    if (imageBytes != null && imageBytes.length > 0) {
      Map<String, Object> imageSource = new LinkedHashMap<>();
      imageSource.put("type", "base64");
      imageSource.put("media_type", mimeType);
      imageSource.put("data", Base64.getEncoder().encodeToString(imageBytes));

      Map<String, Object> imageContent = new LinkedHashMap<>();
      imageContent.put("type", "image");
      imageContent.put("source", imageSource);

      content = List.of(imageContent, textContent);
    } else {
      content = List.of(textContent);
    }

    Map<String, Object> message = new LinkedHashMap<>();
    message.put("role", "user");
    message.put("content", content);

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("model", model);
    body.put("max_tokens", 4096);
    body.put("messages", List.of(message));
    return body;
  }

  private HttpHeaders buildHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("x-api-key", apiKey);
    headers.set("anthropic-version", anthropicVersion);
    return headers;
  }

  private String extractText(Map<String, Object> raw) {
    Object contentObj = raw.get("content");
    if (contentObj instanceof List<?> contentList) {
      for (Object block : contentList) {
        if (block instanceof Map<?, ?> textBlock
            && "text".equals(textBlock.get("type"))
            && textBlock.get("text") instanceof String text) {
          return text;
        }
      }
    }
    throw new BusinessException(
        ErrorCode.AI_PROVIDER_REQUEST_FAILED,
        List.of(
            "Claude 응답에 예상한 text 콘텐츠가 없습니다 (stop_reason=" + raw.get("stop_reason")
                + ", content=" + contentObj + ")"));
  }

  private Map<String, Object> parseJson(String body) {
    try {
      return objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
    } catch (RuntimeException exception) {
      String snippet = body == null ? "null" : body.substring(0, Math.min(body.length(), 300));
      throw new BusinessException(
          ErrorCode.AI_PROVIDER_REQUEST_FAILED, List.of("응답 파싱 실패, 원본 일부: " + snippet));
    }
  }
}
