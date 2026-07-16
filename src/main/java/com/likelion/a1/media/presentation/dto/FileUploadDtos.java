package com.likelion.a1.media.presentation.dto;

public final class FileUploadDtos {
  private FileUploadDtos() {}

  public record UploadResponse(
      String fileType,
      String bucketName,
      String storagePath,
      String publicUrl,
      String originalFilename,
      String storedFilename,
      String mimeType,
      long fileSize) {}
}
