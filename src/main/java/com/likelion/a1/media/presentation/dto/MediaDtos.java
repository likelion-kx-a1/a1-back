package com.likelion.a1.media.presentation.dto;

import java.time.OffsetDateTime;

public final class MediaDtos {
  private MediaDtos() {}

  public record GeneratedAssetResponse(
      Long id,
      Long userId,
      Long chatId,
      Long generationJobId,
      Long responseMessageId,
      Long parentAssetId,
      String assetType,
      String imageCategory,
      String title,
      String prompt,
      String status,
      OffsetDateTime createdAt) {}

  public record AssetFileResponse(
      Long id,
      Long generatedAssetId,
      String fileType,
      String publicUrl,
      String originalFilename,
      String storedFilename,
      String mimeType,
      Long fileSize,
      Integer width,
      Integer height,
      Integer durationSeconds) {}

  public record CreateFolderRequest(Long parentFolderId, String name) {}

  public record StorageFolderResponse(
      Long id,
      Long userId,
      Long parentFolderId,
      String name,
      String status,
      OffsetDateTime createdAt) {}

  public record SaveAssetRequest(Long generatedAssetId, Long folderId, String displayName) {}

  public record SavedAssetResponse(
      Long id,
      Long userId,
      Long folderId,
      Long generatedAssetId,
      String displayName,
      String status,
      OffsetDateTime createdAt) {}

  public record DownloadResponse(
      Long id,
      Long userId,
      Long generatedAssetId,
      Long assetFileId,
      String downloadFilename,
      OffsetDateTime downloadedAt) {}
}
