package com.likelion.a1.media.presentation.dto;

import java.time.OffsetDateTime;
import java.util.List;

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

  public record CreateLibraryProjectRequest(Long parentProjectId, String name) {}

  public record UpdateLibraryProjectRequest(String name) {}

  public record LibraryProjectResponse(
      Long id,
      Long userId,
      Long parentProjectId,
      String name,
      int depth,
      String status,
      StorageFolderResponse imageFolder,
      StorageFolderResponse videoFolder,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt) {}

  public record StorageFolderResponse(
      Long id,
      Long userId,
      Long parentFolderId,
      Long libraryProjectId,
      String name,
      String folderType,
      String assetType,
      String status,
      OffsetDateTime createdAt) {}

  public record SaveAssetRequest(
      Long generatedAssetId, Long libraryProjectId, Long folderId, String displayName) {}

  public record UpdateSavedAssetRequest(String displayName) {}

  public record SavedAssetResponse(
      Long id,
      Long userId,
      Long libraryProjectId,
      Long folderId,
      Long sourceGeneratedAssetId,
      String displayName,
      String assetType,
      String status,
      List<SavedAssetFileResponse> files,
      OffsetDateTime createdAt) {}

  public record SavedAssetFileResponse(
      Long id,
      Long savedAssetId,
      Long sourceAssetFileId,
      String fileType,
      String publicUrl,
      String originalFilename,
      String storedFilename,
      String mimeType,
      Long fileSize,
      Integer width,
      Integer height,
      Integer durationSeconds) {}

  public record DownloadResponse(
      Long id,
      Long userId,
      Long generatedAssetId,
      Long assetFileId,
      String downloadFilename,
      OffsetDateTime downloadedAt) {}
}
