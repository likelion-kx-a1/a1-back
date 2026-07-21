package com.likelion.a1.generation.domain.model;

import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import java.util.List;

/**
 * fal.ai 제출 프롬프트에 결합할 시트(Sheet) 종류. NONE은 원본(또는 Claude 보정) 프롬프트만 사용하고,
 * CHARACTER_EXPRESSION/CUSTOM은 {@link com.likelion.a1.generation.application.service.SheetPromptComposer}가
 * 생성 모드(이미지/비디오)에 따라 정의된 규격의 문구를 원본 뒤에 병합한다(api_3.md 시트 주입 엔진 규격).
 */
public enum SheetType {
  NONE,
  CHARACTER_EXPRESSION,
  CUSTOM;

  /** null/빈 문자열은 NONE으로 간주해 sheetType을 보내지 않는 기존 호출자와 하위 호환을 지킨다. */
  public static SheetType fromRequest(String value) {
    if (value == null || value.isBlank()) {
      return NONE;
    }
    try {
      return SheetType.valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException exception) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT, List.of("알 수 없는 sheetType입니다: " + value));
    }
  }
}
