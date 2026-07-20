package com.likelion.a1.generation.application.port.out;

import java.util.Map;

/**
 * fal.ai 큐 API. modelCode로 이미지·영상 모델을 모두 받는다
 * (예: openai/gpt-image-2, fal-ai/kling-video/o3/pro/text-to-video, bytedance/seedance-2.0/mini/text-to-video).
 */
public interface FalGenerationPort {
  FalGenerationSubmission submit(String modelCode, Map<String, Object> input);

  FalGenerationStatus poll(String modelCode, String externalRequestId);
}
