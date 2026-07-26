package com.likelion.a1.generation.domain.model;

import java.util.List;

/**
 * '캐릭터 설정' 우측 드로어에서 입력받는 선택 필드(charactersheet.md 1.1). 모든 필드는 선택 사항이며,
 * null/blank 필드는 {@link com.likelion.a1.generation.application.service.CharacterSheetPromptTemplate}가
 * 최종 프롬프트에서 해당 섹션 자체를 생략한다(임의로 값을 지어내지 않음).
 */
public record CharacterSheetSettings(
    String name,
    String ageGenderRole,
    String appearance,
    String clothing,
    String accessoriesProps,
    String personality,
    String abilityWorld,
    List<String> keyColors) {

  private static final CharacterSheetSettings EMPTY =
      new CharacterSheetSettings(null, null, null, null, null, null, null, List.of());

  /** FE가 드로어를 아예 열지 않아 characterSettings 자체가 null로 오는 경우를 안전하게 흡수한다. */
  public static CharacterSheetSettings orEmpty(CharacterSheetSettings settings) {
    return settings == null ? EMPTY : settings;
  }
}
