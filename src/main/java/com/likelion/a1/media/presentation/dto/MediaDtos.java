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
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt) {}

  public record LibraryProjectContentsResponse(
      LibraryProjectResponse project,
      List<LibraryProjectResponse> childProjects,
      List<LibraryAssetResponse> assets) {}

  public record LibraryProjectSummaryResponse(
      Long id, String name, Long parentProjectId, int depth) {}

  public record LibrarySourceChatResponse(Long id, Long projectId, String title) {}

  public record LibrarySourceMessageResponse(
      Long id,
      String senderType,
      String messageType,
      String contentText,
      OffsetDateTime createdAt) {}

  public record LibrarySourceGeneratedAssetResponse(
      Long id,
      String title,
      String prompt,
      String assetType,
      String imageCategory,
      OffsetDateTime createdAt) {}

  public record LibraryAssetResponse(
      Long id,
      Long userId,
      String displayName,
      String assetType,
      String status,
      OffsetDateTime createdAt,
      LibraryProjectSummaryResponse libraryProject,
      LibrarySourceChatResponse sourceChat,
      LibrarySourceMessageResponse sourceMessage,
      LibrarySourceGeneratedAssetResponse sourceGeneratedAsset,
      List<SavedAssetFileResponse> files) {}

  public record SaveAssetRequest(
      String sourceType,
      Long generatedAssetId,
      Long chatMessageFileId,
      Long libraryProjectId,
      String displayName) {}

  public record UpdateSavedAssetRequest(String displayName) {}

  public record SavedAssetResponse(
      Long id,
      Long userId,
      Long libraryProjectId,
      Long sourceGeneratedAssetId,
      String sourceType,
      Long sourceChatId,
      Long sourceMessageId,
      Long sourceMessageFileId,
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
