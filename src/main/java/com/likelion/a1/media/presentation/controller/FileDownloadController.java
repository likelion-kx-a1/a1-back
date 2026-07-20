package com.likelion.a1.media.presentation.controller;

import com.likelion.a1.media.application.service.FileDownloadService;
import com.likelion.a1.media.application.service.FileDownloadService.DownloadFile;
import com.likelion.a1.user.infrastructure.security.JwtPrincipal;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FileDownloadController {
  private final FileDownloadService fileDownloadService;

  public FileDownloadController(FileDownloadService fileDownloadService) {
    this.fileDownloadService = fileDownloadService;
  }

  @GetMapping("/api/chats/{chatId}/files/{fileId}/download")
  public ResponseEntity<Resource> downloadChatMessageFile(
      @AuthenticationPrincipal JwtPrincipal principal,
      @PathVariable Long chatId,
      @PathVariable Long fileId) {
    return toDownloadResponse(
        fileDownloadService.downloadChatMessageFile(principal.userId(), chatId, fileId));
  }

  @GetMapping("/api/chats/{chatId}/generated-assets/{generatedAssetId}/files/{fileId}/download")
  public ResponseEntity<Resource> downloadGeneratedAssetFile(
      @AuthenticationPrincipal JwtPrincipal principal,
      @PathVariable Long chatId,
      @PathVariable Long generatedAssetId,
      @PathVariable Long fileId) {
    return toDownloadResponse(
        fileDownloadService.downloadGeneratedAssetFile(
            principal.userId(), chatId, generatedAssetId, fileId));
  }

  private ResponseEntity<Resource> toDownloadResponse(DownloadFile file) {
    ByteArrayResource resource = new ByteArrayResource(file.content());

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.contentType()))
        .contentLength(file.contentLength())
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(file.filename(), StandardCharsets.UTF_8)
                .build()
                .toString())
        .body(resource);
  }
}
