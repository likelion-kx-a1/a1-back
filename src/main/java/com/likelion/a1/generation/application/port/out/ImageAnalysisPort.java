package com.likelion.a1.generation.application.port.out;

/** GPT Vision(멀티모달) API - 이미지를 분석해 구도/질감/무중력 요소를 재현하는 영문 역프롬프트를 추출한다. */
public interface ImageAnalysisPort {
  AiTextGenerationResult analyze(byte[] imageBytes, String mimeType, String instruction);
}
