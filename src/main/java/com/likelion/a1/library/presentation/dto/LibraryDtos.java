package com.likelion.a1.library.presentation.dto;

import java.time.OffsetDateTime;

public final class LibraryDtos {
  private LibraryDtos() {}

  public record CreateLibraryRequest(Long projectId, String name, String description) {}

  public record LibraryResponse(
      Long id,
      Long projectId,
      String name,
      String description,
      String status,
      OffsetDateTime createdAt) {}

  public record CreateMessageRequest(
      Long userId,
      String senderType,
      String messageType,
      String contentText,
      Long parentMessageId) {}

  public record MessageResponse(
      Long id,
      Long libraryId,
      Long userId,
      String senderType,
      String messageType,
      String contentText,
      Long parentMessageId,
      Long generationJobId,
      int sortOrder,
      String status) {}

  public record MessageFileResponse(
      Long id,
      Long messageId,
      String fileType,
      String bucketName,
      String storagePath,
      String publicUrl,
      String mimeType,
      Long fileSize) {}
}
