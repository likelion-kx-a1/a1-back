package com.likelion.a1.chat.presentation.controller;

import com.likelion.a1.chat.application.service.ChatMessageService;
import com.likelion.a1.chat.presentation.dto.ChatDtos.CreateMessageRequest;
import com.likelion.a1.chat.presentation.dto.ChatDtos.MessageFileRequest;
import com.likelion.a1.chat.presentation.dto.ChatDtos.MessageResponse;
import com.likelion.a1.chat.presentation.dto.ChatDtos.UpdateMessageRequest;
import com.likelion.a1.global.response.ApiResponse;
import com.likelion.a1.media.application.service.FileUploadService;
import com.likelion.a1.media.presentation.dto.FileUploadDtos.UploadResponse;
import com.likelion.a1.user.infrastructure.security.JwtPrincipal;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/chats/{chatId}/messages")
public class ChatMessageController {
  private final ChatMessageService chatMessageService;
  private final FileUploadService fileUploadService;

  public ChatMessageController(
      ChatMessageService chatMessageService, FileUploadService fileUploadService) {
    this.chatMessageService = chatMessageService;
    this.fileUploadService = fileUploadService;
  }

  @PostMapping
  public ApiResponse<MessageResponse> create(
      @AuthenticationPrincipal JwtPrincipal principal,
      @PathVariable Long chatId,
      @Valid @RequestBody CreateMessageRequest request) {
    return ApiResponse.success(
        "CHAT_MESSAGE_CREATED",
        "채팅 메시지가 생성되었습니다.",
        chatMessageService.create(principal.userId(), chatId, request));
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<MessageResponse> createMultipart(
      @AuthenticationPrincipal JwtPrincipal principal,
      @PathVariable Long chatId,
      @RequestParam(required = false) String senderType,
      @RequestParam(required = false) String messageType,
      @RequestParam(required = false) String contentText,
      @RequestParam(required = false) String generationType,
      @RequestParam(required = false) String imageCategory,
      @RequestParam(required = false) Long parentMessageId,
      @RequestParam(required = false) List<MultipartFile> files,
      @RequestParam(required = false) List<String> fileTypes) {
    List<MessageFileRequest> uploadedFiles =
        uploadFiles(principal.userId(), chatId, files, fileTypes);

    CreateMessageRequest request =
        new CreateMessageRequest(
            senderType,
            messageType,
            contentText,
            generationType,
            imageCategory,
            parentMessageId,
            uploadedFiles);

    return ApiResponse.success(
        "CHAT_MESSAGE_CREATED",
        "채팅 메시지가 생성되었습니다.",
        chatMessageService.create(principal.userId(), chatId, request));
  }

  @GetMapping
  public ApiResponse<List<MessageResponse>> getMessages(
      @AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long chatId) {
    return ApiResponse.success(
        "CHAT_MESSAGES_FETCHED",
        "채팅 메시지 목록을 조회했습니다.",
        chatMessageService.getMessages(principal.userId(), chatId));
  }

  @PatchMapping("/{messageId}")
  public ApiResponse<MessageResponse> update(
      @AuthenticationPrincipal JwtPrincipal principal,
      @PathVariable Long chatId,
      @PathVariable Long messageId,
      @Valid @RequestBody UpdateMessageRequest request) {
    return ApiResponse.success(
        "CHAT_MESSAGE_UPDATED",
        "채팅 메시지가 수정되었습니다.",
        chatMessageService.update(principal.userId(), chatId, messageId, request));
  }

  @DeleteMapping("/{messageId}")
  public ApiResponse<Void> delete(
      @AuthenticationPrincipal JwtPrincipal principal,
      @PathVariable Long chatId,
      @PathVariable Long messageId) {
    chatMessageService.delete(principal.userId(), chatId, messageId);

    return ApiResponse.success("CHAT_MESSAGE_DELETED", "채팅 메시지가 삭제되었습니다.", null);
  }

  private List<MessageFileRequest> uploadFiles(
      Long userId, Long chatId, List<MultipartFile> files, List<String> fileTypes) {
    if (files == null || files.isEmpty()) {
      return List.of();
    }

    List<MessageFileRequest> uploadedFiles = new ArrayList<>();
    for (int index = 0; index < files.size(); index++) {
      MultipartFile file = files.get(index);
      UploadResponse uploadedFile =
          fileUploadService.uploadChatFile(userId, chatId, file, resolveFileType(file, fileTypes, index));

      uploadedFiles.add(
          new MessageFileRequest(
              uploadedFile.fileType(),
              uploadedFile.bucketName(),
              uploadedFile.storagePath(),
              uploadedFile.publicUrl(),
              uploadedFile.originalFilename(),
              uploadedFile.storedFilename(),
              uploadedFile.mimeType(),
              uploadedFile.fileSize(),
              null,
              null,
              null));
    }

    return uploadedFiles;
  }

  private String resolveFileType(MultipartFile file, List<String> fileTypes, int index) {
    if (fileTypes != null && fileTypes.size() > index && StringUtils.hasText(fileTypes.get(index))) {
      return fileTypes.get(index);
    }

    String contentType = file.getContentType();
    if (!StringUtils.hasText(contentType)) {
      return "FILE";
    }

    if (contentType.startsWith("image/")) {
      return "IMAGE";
    }

    if (contentType.startsWith("video/")) {
      return "VIDEO";
    }

    if (contentType.startsWith("text/") || "application/pdf".equals(contentType)) {
      return "DOCUMENT";
    }

    return "FILE";
  }
}
