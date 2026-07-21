package com.likelion.a1.generation.infrastructure.client.claude;

import com.likelion.a1.generation.application.port.out.AiTextGenerationResult;
import com.likelion.a1.generation.application.port.out.PromptGenerationPort;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** local/default 프로필용 Claude 프롬프트 보정 에뮬레이터. 실제 과금 호출 없이 1초 대기 후 고정된 시네마틱 무중력 프롬프트를 반환한다. */
@Component
@Profile({"local", "default"})
public class MockPromptGenerationAdapter implements PromptGenerationPort {
  private static final String MOCK_PROMPT_TEXT =
      "A cinematic slow-motion shot drifting gracefully in weightless zero-gravity space, "
          + "volumetric dust particles catching refracting cosmic starlight, deep depth of field, "
          + "zero-gravity camera orbiting dolly glide, hyper-realistic zero-g fluid dynamics, "
          + "suspended liquid droplets, inertia decay, cinematic anti-gravity physics, 8k render.";

  @Override
  public AiTextGenerationResult generateFromImage(byte[] imageBytes, String mimeType, String instruction) {
    sleepOneSecond();

    Map<String, Object> raw = new LinkedHashMap<>();
    raw.put("mock", true);
    raw.put("model", "mock-claude-sonnet-5");
    raw.put("instruction", instruction);
    raw.put("mimeType", mimeType);
    raw.put("imageByteSize", imageBytes == null ? 0 : imageBytes.length);

    return new AiTextGenerationResult(MOCK_PROMPT_TEXT, raw);
  }

  private void sleepOneSecond() {
    try {
      Thread.sleep(1000);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
    }
  }
}
