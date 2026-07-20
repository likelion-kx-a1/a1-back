package com.likelion.a1.generation.infrastructure.client.openai;

import com.likelion.a1.generation.application.port.out.AiTextGenerationResult;
import com.likelion.a1.generation.application.port.out.ImageAnalysisPort;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** local/default 프로필용 GPT Vision 역프롬프트 에뮬레이터. 실제 과금 호출 없이 고정된 분석 텍스트를 반환한다. */
@Component
@Profile({"local", "default"})
public class MockImageAnalysisAdapter implements ImageAnalysisPort {
  private static final String MOCK_ANALYSIS_TEXT =
      "A beautiful zero-gravity capture of a floating subject, water droplets forming suspended "
          + "spherical refracting globules in mid-air, volumetric dust motes drifting, "
          + "ink-in-water flow dynamics, dreamlike weightless panning shot, soft studio lighting, "
          + "shallow depth of field.";

  @Override
  public AiTextGenerationResult analyze(byte[] imageBytes, String mimeType, String instruction) {
    Map<String, Object> raw = new LinkedHashMap<>();
    raw.put("mock", true);
    raw.put("model", "mock-gpt-vision");
    raw.put("instruction", instruction);
    raw.put("mimeType", mimeType);
    raw.put("imageByteSize", imageBytes.length);

    return new AiTextGenerationResult(MOCK_ANALYSIS_TEXT, raw);
  }
}
