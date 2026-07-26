package com.likelion.a1.generation.application.port.out;

import com.likelion.a1.generation.domain.model.CharacterSheetSettings;

/**
 * Claude Sonnet API - 캐릭터 시트(Character Design Reference Sheet) 생성 모드에서 ai_enhance=true일 때만
 * 호출된다. 사용자의 핵심 설명(core_description)과 드로어 입력값을 charactersheet.md 2번 규격의 고정
 * 레이아웃 폼으로 재구성한 final_prompt_body를 반환한다.
 */
public interface CharacterSheetPromptPort {
  AiTextGenerationResult generateFinalPromptBody(
      String coreDescription, CharacterSheetSettings settings, boolean hasReferenceImages);
}
