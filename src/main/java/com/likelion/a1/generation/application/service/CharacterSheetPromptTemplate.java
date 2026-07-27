package com.likelion.a1.generation.application.service;

import com.likelion.a1.generation.domain.model.CharacterSheetSettings;
import java.util.ArrayList;
import java.util.List;

/**
 * 캐릭터 시트(Character Design Reference Sheet) 생성 모드의 프롬프트 조립 로직(charactersheet.md 3, 4번
 * 규격). ai_enhance=false일 때 Claude Sonnet을 거치지 않고 사용자 입력값을 고정 폼 구조 문자열로 직접
 * 조립하는 {@link #buildDirect}와, ai_enhance 여부와 무관하게 항상 맨 끝에 결합되는 고정 Requirements
 * 블록을 붙이는 {@link #appendRequirements}를 제공한다.
 */
final class CharacterSheetPromptTemplate {
  private static final String REQUIREMENTS_BLOCK =
      "Requirements:\n"
          + "- Follow the user's explicit request as the highest priority.\n"
          + "- Present the result as a structured character design reference sheet.\n"
          + "- Clearly present the character and all major sheet components.\n"
          + "- Include a readable and organized layout with separated sections.\n"
          + "- Preserve the core visual identity of the character across all views and expressions.\n"
          + "- Do not introduce unrelated characters, objects, or story elements.\n"
          + "- Do not include extra logos, trademarks, or watermarks unless explicitly requested.";

  private CharacterSheetPromptTemplate() {}

  /** ai_enhance 여부와 무관하게 항상 최종 프롬프트 맨 마지막에 고정 Requirements 블록을 결합한다. */
  static String appendRequirements(String promptBody) {
    String safeBody = promptBody == null ? "" : promptBody.trim();
    return safeBody.isEmpty() ? REQUIREMENTS_BLOCK : safeBody + "\n\n" + REQUIREMENTS_BLOCK;
  }

  /**
   * ai_enhance=false일 때 Sonnet을 거치지 않고 사용자 입력값을 고정 폼 구조 문자열로 직접 조립한다
   * (charactersheet.md 3번 buildDirectCharacterSheetPrompt와 동일 로직).
   */
  static String buildDirect(
      String coreDescription, CharacterSheetSettings settings, boolean hasReferenceImages) {
    List<String> sections = new ArrayList<>();

    StringBuilder prose = new StringBuilder();
    prose.append(
            "Create a professional character design reference sheet based on the character "
                + "information provided by the user.")
        .append("\n\n")
        .append(
            "The sheet should present the character in a clean and organized layout designed for "
                + "visual development and production reference.")
        .append("\n\n")
        .append(
            "Include a full-body turnaround, facial expression variations, accessory and prop "
                + "panels, costume and material detail close-ups, a color palette, and short "
                + "character information sections.")
        .append("\n\n")
        .append(
            "The overall aesthetic should reflect the character's concept and world setting while "
                + "maintaining a clear and polished character key-image presentation.")
        .append("\n\n")
        .append(
            "Use a bright, simple, and unobtrusive background so the character and sheet "
                + "components are easy to distinguish.");

    String name = blankToNull(settings.name());
    if (name != null) {
      prose.append("\n\nCharacter name: ").append(name).append(".");
    }
    sections.add(prose.toString());

    sections.add("CORE CONCEPT:\n" + (coreDescription == null ? "" : coreDescription));

    if (name != null) {
      sections.add("CHARACTER NAME:\n" + name);
    }
    addIfPresent(sections, "SUBJECT", settings.ageGenderRole());
    addIfPresent(sections, "APPEARANCE", settings.appearance());

    sections.add(
        "FACE / EXPRESSIONS:\nShow a set of facial expressions appropriate to the character's "
            + "personality and role.");
    sections.add(
        "BODY / TURNAROUND:\nShow consistent full-body front, side, and back views.");

    addIfPresent(sections, "CLOTHING", settings.clothing());
    addIfPresent(sections, "ACCESSORIES / PROPS", settings.accessoriesProps());

    sections.add(
        "DETAIL HIGHLIGHTS:\nShow close-up detail panels for visually important clothing, "
            + "accessories, props, symbols, or material details.");

    List<String> keyColors = settings.keyColors();
    if (keyColors != null && !keyColors.isEmpty()) {
      sections.add("COLOR PALETTE:\n" + String.join(", ", keyColors));
    }
    addIfPresent(sections, "PERSONALITY", settings.personality());
    addIfPresent(sections, "ABILITY / WORLD", settings.abilityWorld());

    if (hasReferenceImages) {
      sections.add(
          "REFERENCE IMAGE INSTRUCTIONS:\n"
              + "- Preserve: facial features, hairstyle, clothing style, key colors.\n"
              + "- Generate or change: elements needed to complete turnaround and detail views.\n"
              + "- Do not reproduce unrelated reference-image elements.");
    }

    sections.add(
        "LAYOUT:\nUse a clean grid-based character reference sheet layout with clearly separated "
            + "sections for character information, full-body turnaround, expressions, accessories "
            + "and props, detail close-ups, color palette, and character or world notes.");
    sections.add(
        "STYLE:\nPolished digital character-design illustration style appropriate to the "
            + "character concept.");
    sections.add(
        "LIGHTING:\nUse even, clean, studio-like lighting suitable for a character design "
            + "reference sheet.");
    sections.add(
        "BACKGROUND:\nUse a simple light background with subtle panel divisions and organized "
            + "information areas.");
    sections.add("ASPECT RATIO:\n16:9");

    sections.add(
        "CONSTRAINTS - MUST KEEP:\nConsistent character identity across all views and "
            + "expressions.");
    sections.add(
        "CONSTRAINTS - AVOID:\nInconsistent character identity between views, distorted anatomy, "
            + "blurry details, cluttered panel organization, unrelated characters, extra logos, "
            + "trademarks, or watermarks.");

    return String.join("\n\n", sections);
  }

  private static void addIfPresent(List<String> sections, String heading, String value) {
    String safeValue = blankToNull(value);
    if (safeValue != null) {
      sections.add(heading + ":\n" + safeValue);
    }
  }

  private static String blankToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
