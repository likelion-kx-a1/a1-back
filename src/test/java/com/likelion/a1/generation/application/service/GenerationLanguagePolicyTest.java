package com.likelion.a1.generation.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GenerationLanguagePolicyTest {
  @Test
  void requiresKoreanTextForImageByDefault() {
    String result = GenerationLanguagePolicy.forImagePrompt("A poster in a cafe", "카페 포스터");

    assertThat(result).contains("모든 글자를 자연스럽고 정확한 한국어로 작성하세요");
  }

  @Test
  void preservesExplicitEnglishImageRequest() {
    String result =
        GenerationLanguagePolicy.forImagePrompt(
            "A poster in a cafe with English copy", "포스터 문구는 영어로 작성해줘");

    assertThat(result).isEqualTo("A poster in a cafe with English copy");
  }

  @Test
  void requiresExactlyTwoKoreanReversePromptsByDefault() {
    String result = GenerationLanguagePolicy.forReversePrompt("이 이미지를 분석해줘");

    assertThat(result)
        .contains("정확히 두 개")
        .contains("모든 문장을 자연스러운 한국어로 작성하세요");
  }

  @Test
  void preservesExplicitEnglishReversePromptRequest() {
    String instruction = "Return the result in English";

    assertThat(GenerationLanguagePolicy.forReversePrompt(instruction)).isEqualTo(instruction);
  }
}
