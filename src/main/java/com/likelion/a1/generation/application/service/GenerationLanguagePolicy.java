package com.likelion.a1.generation.application.service;

import java.util.regex.Pattern;

/** 생성 결과의 기본 언어를 한국어로 유지하되 명시적인 영어 요청은 보존한다. */
final class GenerationLanguagePolicy {
  private static final Pattern ENGLISH_REQUEST =
      Pattern.compile(
          "(영어|영문)\\s*(로|으로|텍스트|문구|자막|작성|표기|출력)|"
              + "\\b(in\\s+english|english\\s+(text|copy|caption|words?|language))\\b",
          Pattern.CASE_INSENSITIVE);

  private static final String KOREAN_IMAGE_TEXT_REQUIREMENT =
      "\n\n언어 요구사항: 이미지 안에 간판, 자막, 라벨, 포스터 등 읽을 수 있는 글자가 포함된다면 "
          + "모든 글자를 자연스럽고 정확한 한국어로 작성하세요. 영어 문구나 임의의 알파벳을 넣지 마세요.";

  private static final String KOREAN_REVERSE_PROMPT_REQUIREMENT =
      "\n\n출력 언어 요구사항: 서로 다른 이미지 생성 프롬프트를 정확히 두 개만 제공하고, "
          + "두 프롬프트의 모든 문장을 자연스러운 한국어로 작성하세요. 영어 문장, 설명, 머리말은 넣지 마세요. "
          + "마크다운 없이 두 문자열만 포함한 JSON 배열로 출력하세요.";

  private GenerationLanguagePolicy() {}

  static String forImagePrompt(String prompt, String originalUserPrompt) {
    if (prompt == null || prompt.isBlank() || explicitlyRequestsEnglish(originalUserPrompt)) {
      return prompt;
    }
    return prompt + KOREAN_IMAGE_TEXT_REQUIREMENT;
  }

  static String forReversePrompt(String instruction) {
    if (explicitlyRequestsEnglish(instruction)) {
      return instruction;
    }
    return instruction + KOREAN_REVERSE_PROMPT_REQUIREMENT;
  }

  static boolean explicitlyRequestsEnglish(String text) {
    return text != null && !text.isBlank() && ENGLISH_REQUEST.matcher(text).find();
  }
}
