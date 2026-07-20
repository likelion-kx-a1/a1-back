package com.likelion.a1.generation.application.service;

import com.likelion.a1.generation.domain.model.GenerationType;
import com.likelion.a1.generation.domain.model.SheetType;
import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import java.util.List;
import java.util.Map;

/**
 * 시트(Sheet) 유형과 생성 모드(이미지/비디오)에 따라 fal.ai에 보낼 최종 prompt를 조건부로 결합한다
 * (api_3.md 시트 주입 엔진 규격).
 *
 * <ul>
 *   <li>NONE: 원본(또는 Claude 보정) 프롬프트를 그대로 반환한다.
 *   <li>CHARACTER_EXPRESSION + VIDEO_GENERATION: sheetValue(HARMONIOUS/CHILLY/MYSTERIOUS/DRAMATIC)에
 *       대응하는 사전 정의 상황 분위기 물리 문구 하나만 결합한다.
 *   <li>CHARACTER_EXPRESSION + IMAGE_GENERATION(및 IMAGE_VARIATION): sheetValue와 무관하게 9종 감정을
 *       3x3 그리드로 배치하라는 고정 지시문을 결합한다.
 *   <li>CUSTOM: 생성 모드와 무관하게 sheetValue(사용자가 직접 편집한 텍스트)를 그대로 결합한다.
 * </ul>
 */
final class SheetPromptComposer {
  private static final Map<String, String> VIDEO_ATMOSPHERE_PRESETS =
      Map.of(
          "HARMONIOUS",
              "warm and harmonious atmosphere, soft golden hour lighting, gentle smiling faces, "
                  + "comforting cinematic depth",
          "CHILLY",
              "cold and chilly atmosphere, desaturated cool blue tones, distant gazes, silent tension, "
                  + "sharp sterile shadows",
          "MYSTERIOUS",
              "mysterious and ethereal atmosphere, magical glowing dust, deep indigo twilight, "
                  + "awe-inspiring silence",
          "DRAMATIC",
              "intense dramatic atmosphere, heavy cinematic shadows, high contrast, ticking momentum, "
                  + "suspenseful focus");

  private static final String IMAGE_CHARACTER_GRID_PROMPT =
      "An expression model sheet, character design sheet showing a 3x3 grid display of 9 distinct "
          + "facial expressions for the same character in a unified layout: "
          + "[1] JOY: joy with warm smile, crinkling eyes, "
          + "[2] ANGER: intense rage, furrowed brow, clenching jaw, "
          + "[3] SADNESS: sadness, tear suspended globules at the corner of eyes, "
          + "[4] EXCITEMENT: pure excitement, wide delighted eyes catching light, "
          + "[5] SURPRISE: wide-eyed astonishment, slightly parted lips in shock, "
          + "[6] FEAR: pure terror, dilated pupils reflecting shadows, tense jaw, "
          + "[7] DISGUST: contempt, wrinkled nose, narrowed eyes in disgust, "
          + "[8] CALMNESS: serene tranquil face, muscle tension dissolved in calm weightlessness, "
          + "[9] DESIRE: intense longing, focused gaze looking toward the cosmic horizon - "
          + "rendered in a clean grid arrangement, highly detailed, consistent art style";

  private SheetPromptComposer() {}

  static String compose(String basePrompt, GenerationType jobType, String sheetType, String sheetValue) {
    String safeBasePrompt = basePrompt == null ? "" : basePrompt;
    SheetType type = SheetType.fromRequest(sheetType);

    return switch (type) {
      case NONE -> safeBasePrompt;
      case CUSTOM -> appendCustom(safeBasePrompt, sheetValue);
      case CHARACTER_EXPRESSION -> appendCharacterExpression(safeBasePrompt, jobType, sheetValue);
    };
  }

  private static String appendCustom(String basePrompt, String sheetValue) {
    if (sheetValue == null || sheetValue.isBlank()) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT, List.of("sheetType=CUSTOM이면 sheetValue(편집한 텍스트)가 필요합니다."));
    }
    return join(basePrompt, sheetValue.trim());
  }

  private static String appendCharacterExpression(String basePrompt, GenerationType jobType, String sheetValue) {
    String mediaType = jobType == null ? null : jobType.mediaType();

    if ("VIDEO".equals(mediaType)) {
      String presetKey = sheetValue == null ? "" : sheetValue.trim().toUpperCase();
      String preset = VIDEO_ATMOSPHERE_PRESETS.get(presetKey);
      if (preset == null) {
        throw new BusinessException(
            ErrorCode.INVALID_INPUT,
            List.of(
                "비디오 CHARACTER_EXPRESSION 시트의 sheetValue는 "
                    + VIDEO_ATMOSPHERE_PRESETS.keySet()
                    + " 중 하나여야 합니다: " + sheetValue));
      }
      return join(basePrompt, preset);
    }

    if ("IMAGE".equals(mediaType)) {
      return join(basePrompt, IMAGE_CHARACTER_GRID_PROMPT);
    }

    return basePrompt;
  }

  private static String join(String basePrompt, String addition) {
    return basePrompt.isBlank() ? addition : basePrompt + ", " + addition;
  }
}
