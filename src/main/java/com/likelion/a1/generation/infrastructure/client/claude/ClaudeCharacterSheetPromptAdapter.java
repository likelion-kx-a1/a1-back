package com.likelion.a1.generation.infrastructure.client.claude;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.likelion.a1.generation.application.port.out.AiTextGenerationResult;
import com.likelion.a1.generation.application.port.out.CharacterSheetPromptPort;
import com.likelion.a1.generation.domain.model.CharacterSheetSettings;
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

/**
 * prod 프로필용 실제 Anthropic Messages API 연동. 캐릭터 시트(Character Design Reference Sheet)
 * 생성 모드에서 ai_enhance=true일 때, 사용자 입력(core_description + 드로어 설정)을
 * charactersheet.md 2번 규격의 고정 캐릭터 시트 레이아웃 폼으로 재구성한 final_prompt_body를 받아온다.
 */
@Component
@Profile("prod")
public class ClaudeCharacterSheetPromptAdapter implements CharacterSheetPromptPort {
  private static final String SYSTEM_PROMPT =
      """
      You are a prompt optimizer for a character sheet generation feature using OpenAI GPT Image 2.

      Your task is to convert the user's character description and optional character settings into a production-ready prompt for a structured character design reference sheet.

      The output must be a character design reference sheet, not a general character illustration or story scene.

      The final prompt should instruct the image model to generate:
      - a full-body character turnaround,
      - multiple facial expressions,
      - accessory and prop panels,
      - costume and material detail close-ups,
      - a color palette,
      - and short character information sections.

      Follow these strict rules:

      1. Preserve every explicit detail provided by the user. Do not change the core concept, appearance, or world setting.
      2. Treat user inputs as absolute source of truth. User inputs override reference images if they conflict.
      3. Keep the layout form and section structure strictly identical to the requested template format.
      4. The aspect ratio is ALWAYS 16:9.
      5. IF NO CHARACTER NAME IS PROVIDED, DO NOT INVENT A NAME. Omit the "CHARACTER NAME:" section completely and omit "Character name: [name]" in the prose block.
      6. When optional fields are missing, add only restrained, visually coherent enhancements appropriate for the world setting to increase rendering quality and confidence. Do not invent overly specific backstories, specific real-world brands, or family trees.
      7. Return ONLY a valid JSON object in this exact format:
         {
         "final_prompt_body": "The generated prompt body string"
         }

      Construct the final prompt body according to the exact section structure below:

      Create a professional character design reference sheet based on the character information provided by the user.

      The sheet should present the character in a clean and organized layout designed for visual development and production reference.

      Include a full-body turnaround, facial expression variations, accessory and prop panels, costume and material detail close-ups, a color palette, and short character information sections.

      The overall aesthetic should reflect the character's concept and world setting while maintaining a clear and polished character key-image presentation.

      Use a bright, simple, and unobtrusive background so the character and sheet components are easy to distinguish.
      (Append "Character name: [Name]." ONLY if a character name was provided by the user.)

      CORE CONCEPT:
      (User's core character description)

      CHARACTER NAME:
      (Include ONLY if provided. Otherwise omit this entire heading)

      SUBJECT:
      (Include age, gender, role if provided or conservative inference from core concept)

      APPEARANCE:
      (Detailed appearance features enhanced for visual clarity)

      FACE / EXPRESSIONS:
      (Facial expression variations matching the personality and role)

      BODY / TURNAROUND:
      Show consistent full-body front, side, and back views.

      CLOTHING:
      (Detailed costume specs based on input)

      ACCESSORIES / PROPS:
      (Key props and items)

      DETAIL HIGHLIGHTS:
      (Close-up targets for materials, symbols, or intricate items)

      COLOR PALETTE:
      (Key colors provided or derived)

      PERSONALITY:
      (Include ONLY if provided or inferable)

      ABILITY / WORLD:
      (Include ONLY if provided)

      REFERENCE IMAGE INSTRUCTIONS:
      (Include ONLY if reference images exist)
      - Preserve: (visual features to keep from reference image)
      - Generate or change: (elements to modify)
      - Do not reproduce unrelated reference-image elements.

      LAYOUT:
      Use a clean grid-based character reference sheet layout with clearly separated sections for character information, full-body turnaround, expressions, accessories and props, detail close-ups, color palette, and character or world notes.

      STYLE:
      Polished digital character-design illustration style appropriate to the character concept.

      LIGHTING:
      Use even, clean, studio-like lighting suitable for a character design reference sheet.

      BACKGROUND:
      Use a simple light background with subtle panel divisions and organized information areas.

      ASPECT RATIO:
      16:9

      CONSTRAINTS - MUST KEEP:
      (Summary of key visual identifiers that must remain consistent across panels)

      CONSTRAINTS - AVOID:
      Inconsistent character identity between views, distorted anatomy, blurry details, cluttered panel organization, unrelated characters, extra logos, trademarks, or watermarks.
      """;

  private final RestTemplate restTemplate = new RestTemplate();
  private final ObjectMapper objectMapper;
  private final String baseUrl;
  private final String apiKey;
  private final String model;
  private final String anthropicVersion;

  public ClaudeCharacterSheetPromptAdapter(
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
  public AiTextGenerationResult generateFinalPromptBody(
      String coreDescription, CharacterSheetSettings settings, boolean hasReferenceImages) {
    ResponseEntity<String> response =
        restTemplate.exchange(
            baseUrl + "/v1/messages",
            HttpMethod.POST,
            new HttpEntity<>(
                buildRequestBody(coreDescription, settings, hasReferenceImages), buildHeaders()),
            String.class);

    Map<String, Object> raw = parseJson(response.getBody());
    String finalPromptBody = extractFinalPromptBody(extractText(raw));
    return new AiTextGenerationResult(finalPromptBody, raw);
  }

  private Map<String, Object> buildRequestBody(
      String coreDescription, CharacterSheetSettings settings, boolean hasReferenceImages) {
    Map<String, Object> characterSettings = new LinkedHashMap<>();
    characterSettings.put("name", settings.name());
    characterSettings.put("age_gender_role", settings.ageGenderRole());
    characterSettings.put("appearance", settings.appearance());
    characterSettings.put("clothing", settings.clothing());
    characterSettings.put("accessories_props", settings.accessoriesProps());
    characterSettings.put("personality", settings.personality());
    characterSettings.put("ability_world", settings.abilityWorld());
    characterSettings.put("key_colors", settings.keyColors());

    Map<String, Object> userInput = new LinkedHashMap<>();
    userInput.put("core_description", coreDescription);
    userInput.put("character_settings", characterSettings);
    userInput.put("has_reference_images", hasReferenceImages);

    Map<String, Object> textContent = new LinkedHashMap<>();
    textContent.put("type", "text");
    textContent.put("text", writeJson(userInput));

    Map<String, Object> message = new LinkedHashMap<>();
    message.put("role", "user");
    message.put("content", List.of(textContent));

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("model", model);
    body.put("max_tokens", 4096);
    body.put("system", SYSTEM_PROMPT);
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

  /** Claude가 지시(7번 규칙)를 어기고 ```json 코드펜스로 감싸는 경우를 대비해 벗겨낸 뒤 JSON으로 파싱한다. */
  private String extractFinalPromptBody(String responseText) {
    String candidate = stripCodeFence(responseText.trim());
    Map<String, Object> parsed = parseJson(candidate);
    if (parsed.get("final_prompt_body") instanceof String finalPromptBody
        && !finalPromptBody.isBlank()) {
      return finalPromptBody;
    }
    throw new BusinessException(
        ErrorCode.AI_PROVIDER_REQUEST_FAILED,
        List.of("Claude 응답에 final_prompt_body가 없습니다: " + candidate));
  }

  private String stripCodeFence(String text) {
    if (!text.startsWith("```")) {
      return text;
    }
    int firstNewline = text.indexOf('\n');
    String withoutOpeningFence = firstNewline == -1 ? "" : text.substring(firstNewline + 1);
    int closingFenceIndex = withoutOpeningFence.lastIndexOf("```");
    return closingFenceIndex == -1
        ? withoutOpeningFence.trim()
        : withoutOpeningFence.substring(0, closingFenceIndex).trim();
  }

  private String writeJson(Map<String, Object> value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (RuntimeException exception) {
      throw new BusinessException(
          ErrorCode.AI_PROVIDER_REQUEST_FAILED, List.of("요청 JSON 직렬화 실패: " + exception.getMessage()));
    }
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
