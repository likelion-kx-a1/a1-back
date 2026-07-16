package com.likelion.a1.generation.presentation.dto;

import com.likelion.a1.chat.presentation.dto.ChatDtos.MessageResponse;

public final class GenerationResultDtos {
  private GenerationResultDtos() {}

  public record AssistantResultResponse(MessageResponse message, Long generatedAssetId) {}
}
