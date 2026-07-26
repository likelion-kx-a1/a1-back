package com.likelion.a1.generation.infrastructure.client.claude;

import com.likelion.a1.generation.application.port.out.AiTextGenerationResult;
import com.likelion.a1.generation.application.port.out.CharacterSheetPromptPort;
import com.likelion.a1.generation.domain.model.CharacterSheetSettings;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * local/default 프로필용 캐릭터 시트 Claude 보정 에뮬레이터. 실제 과금 호출 없이 1초 대기 후, 입력값을
 * 반영한 고정 형태의 final_prompt_body를 반환한다.
 */
@Component
@Profile({"local", "default"})
public class MockCharacterSheetPromptAdapter implements CharacterSheetPromptPort {
  @Override
  public AiTextGenerationResult generateFinalPromptBody(
      String coreDescription, CharacterSheetSettings settings, boolean hasReferenceImages) {
    sleepOneSecond();

    String name = settings.name() == null || settings.name().isBlank() ? null : settings.name().trim();
    String mockBody =
        "CORE CONCEPT:\n"
            + (coreDescription == null ? "" : coreDescription)
            + "\n\n(mock Claude Sonnet character sheet enhancement"
            + (name != null ? " for " + name : "")
            + ", reference images: "
            + hasReferenceImages
            + ")\n\nASPECT RATIO:\n16:9";

    Map<String, Object> raw = new LinkedHashMap<>();
    raw.put("mock", true);
    raw.put("model", "mock-claude-sonnet-5");
    raw.put("coreDescription", coreDescription);
    raw.put("hasReferenceImages", hasReferenceImages);

    return new AiTextGenerationResult(mockBody, raw);
  }

  private void sleepOneSecond() {
    try {
      Thread.sleep(1000);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
    }
  }
}
