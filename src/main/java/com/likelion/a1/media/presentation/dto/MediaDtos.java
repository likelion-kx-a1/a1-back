package com.likelion.a1.media.presentation.dto;

import java.time.OffsetDateTime;

public final class MediaDtos {
  private MediaDtos() {}

  public record GeneratedMediaResponse(
      Long id,
      Long libraryId,
      Long generationJobId,
      Long responseMessageId,
      Long parentMediaId,
      String mediaType,
      String title,
      String prompt,
      boolean saved,
      String status,
      OffsetDateTime createdAt) {}

  public record StorageFileResponse(
      Long id,
      Long generatedMediaId,
      String fileType,
      String bucketName,
      String storagePath,
      String publicUrl,
      String mimeType,
      Long fileSize,
      Integer width,
      Integer height,
      Integer durationSeconds) {}
}
