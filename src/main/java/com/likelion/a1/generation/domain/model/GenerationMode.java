package com.likelion.a1.generation.domain.model;

/**
 * fal.ai 제출({@code /fal-jobs}) 요청의 생성 모드. CHARACTER_SHEET는 캐릭터 디자인 레퍼런스 시트
 * 전용 파이프라인(charactersheet.md 규격)으로 분기하고, 그 외 값(또는 생략)은 기존 시트 주입 엔진
 * ({@link SheetType})이 그대로 동작하는 기본 경로로 취급한다.
 */
public enum GenerationMode {
  DEFAULT,
  CHARACTER_SHEET;

  /** null이거나 "character_sheet"(대소문자 무관)가 아니면 모두 DEFAULT로 간주해 기존 호출자와 호환된다. */
  public static GenerationMode fromRequest(String value) {
    if (value == null) {
      return DEFAULT;
    }
    return "character_sheet".equalsIgnoreCase(value.trim()) ? CHARACTER_SHEET : DEFAULT;
  }
}
