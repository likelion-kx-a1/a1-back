package com.likelion.a1.chat.presentation.dto;

import java.time.OffsetDateTime;

public final class ChatDtos {
  private ChatDtos() {}

  public record CreateChatRequest(
      Long projectId, String title, String generationType, String imageCategory) {}

  public record ChatResponse(
      Long id,
      Long userId,
      Long projectId,
      String title,
      String generationType,
      String imageCategory,
      Long firstMessageId,
      boolean generating,
      String status,
      OffsetDateTime createdAt) {}

  public record CreateMessageRequest(
      String senderType,
      String messageType,
      String contentText,
      Long parentMessageId) {}

  public record MessageResponse(
      Long id,
      Long chatId,
      Long userId,
      String senderType,
      String messageType,
      String contentText,
      Long parentMessageId,
      Long generationJobId,
      Long generatedAssetId,
      int sortOrder,
      String status,
      OffsetDateTime createdAt) {}

  public record MessageFileResponse(
      Long id,
      Long messageId,
      String fileType,
      String publicUrl,
      String originalFilename,
      String storedFilename,
      String mimeType,
      Long fileSize,
      Integer width,
      Integer height,
      Integer durationSeconds) {}
}
