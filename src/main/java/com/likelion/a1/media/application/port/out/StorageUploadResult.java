package com.likelion.a1.media.application.port.out;

public record StorageUploadResult(
    String bucketName,
    String storagePath,
    String publicUrl,
    String originalFilename,
    String storedFilename,
    String mimeType,
    long fileSize
) {}
