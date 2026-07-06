package com.likelion.a1.media.presentation.dto;

import java.time.OffsetDateTime;

public final class MediaDtos {
  private MediaDtos() {}

  public record GeneratedMediaResponse(
      Long id,
      Long userId,
      Long chatId,
      Long generationJobId,
      Long responseMessageId,
      Long parentMediaId,
      String mediaType,
      String title,
      String prompt,
      String status,
      OffsetDateTime createdAt) {}

  public record GeneratedMediaFileResponse(
      Long id,
      Long generatedMediaId,
      String fileType,
      String publicUrl,
      String originalFilename,
      String storedFilename,
      String mimeType,
      Long fileSize,
      Integer width,
      Integer height,
      Integer durationSeconds) {}

  public record CreateFolderRequest(Long projectId, Long parentFolderId, String name) {}

  public record FolderResponse(
      Long id,
      Long userId,
      Long projectId,
      Long parentFolderId,
      String name,
      String status,
      OffsetDateTime createdAt) {}

  public record SaveMediaRequest(
      Long generatedMediaId, Long projectId, Long folderId, String displayName) {}

  public record SavedMediaResponse(
      Long id,
      Long userId,
      Long generatedMediaId,
      Long projectId,
      Long folderId,
      String displayName,
      OffsetDateTime createdAt) {}

  public record DownloadResponse(
      Long id,
      Long userId,
      Long generatedMediaId,
      Long generatedMediaFileId,
      String downloadFilename,
      OffsetDateTime downloadedAt) {}
}
