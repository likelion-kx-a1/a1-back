package com.likelion.a1.generation.application.port.out;

/** Claude Sonnet API - 참고 이미지와 한국어 지시문을 무중력 시네마틱 영문 프롬프트로 보정한다. */
public interface PromptGenerationPort {
  AiTextGenerationResult generateFromImage(byte[] imageBytes, String mimeType, String instruction);
}
