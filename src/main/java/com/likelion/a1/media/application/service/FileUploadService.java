package com.likelion.a1.media.application.service;

import com.likelion.a1.chat.application.service.ChatService;
import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import com.likelion.a1.media.application.port.out.MediaStoragePort;
import com.likelion.a1.media.application.port.out.StorageUploadCommand;
import com.likelion.a1.media.application.port.out.StorageUploadResult;
import com.likelion.a1.media.presentation.dto.FileUploadDtos.UploadResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class FileUploadService {
  private static final List<String> ALLOWED_FILE_TYPES = List.of("IMAGE", "VIDEO", "DOCUMENT", "FILE");

  private final ChatService chatService;
  private final MediaStoragePort mediaStoragePort;

  public FileUploadService(ChatService chatService, MediaStoragePort mediaStoragePort) {
    this.chatService = chatService;
    this.mediaStoragePort = mediaStoragePort;
  }

  public UploadResponse uploadChatFile(Long userId, Long chatId, MultipartFile file, String fileType) {
    chatService.findOwnedChat(userId, chatId);
    validateFile(file);

    String normalizedFileType = normalizeFileType(fileType);
    StorageUploadResult result =
        mediaStoragePort.upload(
            new StorageUploadCommand(
                readBytes(file),
                file.getOriginalFilename(),
                normalizeContentType(file.getContentType()),
                null,
                buildChatInputDirectory(userId, chatId, normalizedFileType)));

    return new UploadResponse(
        normalizedFileType,
        result.bucketName(),
        result.storagePath(),
        result.publicUrl(),
        result.originalFilename(),
        result.storedFilename(),
        result.mimeType(),
        result.fileSize());
  }

  private void validateFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT);
    }
  }

  private byte[] readBytes(MultipartFile file) {
    try {
      return file.getBytes();
    } catch (IOException exception) {
      throw new BusinessException(ErrorCode.INVALID_INPUT);
    }
  }

  private String normalizeFileType(String fileType) {
    if (!StringUtils.hasText(fileType)) {
      return "FILE";
    }

    String normalized = fileType.trim().toUpperCase();
    if (!ALLOWED_FILE_TYPES.contains(normalized)) {
      throw new BusinessException(ErrorCode.INVALID_INPUT);
    }

    return normalized;
  }

  private String normalizeContentType(String contentType) {
    if (!StringUtils.hasText(contentType)) {
      return "application/octet-stream";
    }

    return contentType.trim();
  }

  private String buildChatInputDirectory(Long userId, Long chatId, String fileType) {
    return "users/"
        + userId
        + "/chats/"
        + chatId
        + "/input/"
        + resolveInputDirectoryName(fileType);
  }

  private String resolveInputDirectoryName(String fileType) {
    if ("IMAGE".equals(fileType)) {
      return "images";
    }

    if ("VIDEO".equals(fileType)) {
      return "videos";
    }

    return "files";
  }
}
