package com.likelion.a1.generation.domain.model;

public enum GenerationType {
  IMAGE_GENERATION,
  VIDEO_GENERATION,
  REVERSE_PROMPT,
  IMAGE_VARIATION,
  PROMPT_REGENERATION;

  public String mediaType() {
    return switch (this) {
      case IMAGE_GENERATION, IMAGE_VARIATION -> "IMAGE";
      case VIDEO_GENERATION -> "VIDEO";
      case REVERSE_PROMPT, PROMPT_REGENERATION -> "TEXT";
    };
  }
}
