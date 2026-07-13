package com.likelion.a1.chat.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public final class ChatDtos {
  private ChatDtos() {}

  public record CreateChatRequest(
      Long projectId,

      @NotBlank(message = "채팅 제목은 필수입니다.")
      @Size(max = 255, message = "채팅 제목은 255자 이하여야 합니다.")
      String title,

      String generationType,

      String imageCategory) {}

  public record UpdateChatRequest(
      @NotBlank(message = "채팅 제목은 필수입니다.")
      @Size(max = 255, message = "채팅 제목은 255자 이하여야 합니다.")
      String title) {}

  public record ChatResponse(
      Long chatId,
      Long projectId,
      String title,
      String generationType,
      String imageCategory,
      Long firstMessageId,
      boolean isGenerating,
      String status,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt) {}

  public record CreateMessageRequest(
      @Pattern(regexp = "USER|ASSISTANT|SYSTEM", message = "senderType은 USER, ASSISTANT, SYSTEM만 가능합니다.")
      String senderType,

      @Pattern(regexp = "TEXT|IMAGE|VIDEO|FILE", message = "messageType은 TEXT, IMAGE, VIDEO, FILE만 가능합니다.")
      String messageType,

      @NotBlank(message = "메시지 내용은 필수입니다.")
      String contentText,

      Long parentMessageId) {}

  public record UpdateMessageRequest(
      @NotBlank(message = "메시지 내용은 필수입니다.")
      String contentText) {}

  public record MessageResponse(
      Long messageId,
      Long chatId,
      String senderType,
      String messageType,
      String contentText,
      Long parentMessageId,
      Long generationJobId,
      Long generatedAssetId,
      int sortOrder,
      String status,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt) {}

  public record MessageFileResponse(
      Long fileId,
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
