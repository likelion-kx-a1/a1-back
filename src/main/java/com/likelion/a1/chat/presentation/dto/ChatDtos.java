package com.likelion.a1.chat.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;

public final class ChatDtos {
  private ChatDtos() {}

  public record CreateChatRequest(
      Long projectId,

      @NotBlank(message = "채팅 제목은 필수입니다.")
      @Size(max = 255, message = "채팅 제목은 255자 이하여야 합니다.")
      String title,

      @Pattern(regexp = "IMAGE|VIDEO", message = "generationType은 IMAGE 또는 VIDEO만 가능합니다.")
      String generationType,

      @Pattern(
          regexp = "CHARACTER|BACKGROUND|ETC",
          message = "imageCategory는 CHARACTER, BACKGROUND, ETC만 가능합니다.")
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

  public record ChatDetailResponse(
      Long chatId,
      Long projectId,
      String title,
      String generationType,
      String imageCategory,
      Long firstMessageId,
      boolean isGenerating,
      String status,
      List<MessageResponse> messages,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt) {}

  public record CreateMessageRequest(
      @Pattern(regexp = "USER|ASSISTANT|AI|SYSTEM", message = "senderType이 올바르지 않습니다.")
      String senderType,

      @Pattern(
          regexp = "TEXT|IMAGE|VIDEO|FILE|MIXED|ASSET|ASSET_REQUEST",
          message = "messageType이 올바르지 않습니다.")
      String messageType,

      String contentText,

      @Pattern(regexp = "IMAGE|VIDEO", message = "generationType은 IMAGE 또는 VIDEO만 가능합니다.")
      String generationType,

      @Pattern(
          regexp = "CHARACTER|BACKGROUND|ETC",
          message = "imageCategory는 CHARACTER, BACKGROUND, ETC만 가능합니다.")
      String imageCategory,

      Long parentMessageId,

      List<@Valid MessageFileRequest> files) {}

  public record MessageFileRequest(
      @Pattern(regexp = "IMAGE|VIDEO|DOCUMENT|FILE", message = "fileType이 올바르지 않습니다.")
      String fileType,

      @NotBlank(message = "bucketName은 필수입니다.")
      String bucketName,

      @NotBlank(message = "storagePath는 필수입니다.")
      String storagePath,

      String publicUrl,
      String originalFilename,
      String storedFilename,
      String mimeType,
      Long fileSize,
      Integer width,
      Integer height,
      Integer durationSeconds) {}

  public record UpdateMessageRequest(
      @NotBlank(message = "메시지 내용은 필수입니다.")
      String contentText) {}

  public record MessageResponse(
      Long messageId,
      Long chatId,
      String senderType,
      String messageType,
      String contentText,
      String generationType,
      String imageCategory,
      Long parentMessageId,
      Long generationJobId,
      Long generatedAssetId,
      int sortOrder,
      String status,
      List<MessageFileResponse> files,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt) {}

  public record MessageFileResponse(
      Long fileId,
      Long messageId,
      String fileType,
      String bucketName,
      String storagePath,
      String publicUrl,
      String originalFilename,
      String storedFilename,
      String mimeType,
      Long fileSize,
      Integer width,
      Integer height,
      Integer durationSeconds,
      OffsetDateTime createdAt) {}
}
