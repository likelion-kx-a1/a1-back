package com.likelion.a1.media.application.port.out;

public record StorageUploadCommand(
    byte[] content,
    String originalFilename,
    String contentType,
    String extension,
    String directory
) {}
