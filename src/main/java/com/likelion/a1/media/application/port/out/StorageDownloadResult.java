package com.likelion.a1.media.application.port.out;

public record StorageDownloadResult(byte[] content, String contentType, Long contentLength) {}
