package com.likelion.a1.media.presentation.controller;

import com.likelion.a1.global.response.ApiResponse;
import com.likelion.a1.media.application.service.FileUploadService;
import com.likelion.a1.media.presentation.dto.FileUploadDtos.UploadResponse;
import com.likelion.a1.user.infrastructure.security.JwtPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class FileUploadController {
  private final FileUploadService fileUploadService;

  public FileUploadController(FileUploadService fileUploadService) {
    this.fileUploadService = fileUploadService;
  }

  @PostMapping("/api/chats/{chatId}/files")
  public ApiResponse<UploadResponse> uploadChatFile(
      @AuthenticationPrincipal JwtPrincipal principal,
      @PathVariable Long chatId,
      @RequestParam MultipartFile file,
      @RequestParam(required = false) String fileType) {
    return ApiResponse.success(
        "FILE_UPLOADED",
        "파일이 업로드되었습니다.",
        fileUploadService.uploadChatFile(principal.userId(), chatId, file, fileType));
  }
}
