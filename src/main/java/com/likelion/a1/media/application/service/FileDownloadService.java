package com.likelion.a1.media.application.service;

import com.likelion.a1.chat.application.service.ChatService;
import com.likelion.a1.chat.domain.model.ChatMessage;
import com.likelion.a1.chat.domain.model.ChatMessageFile;
import com.likelion.a1.chat.domain.repository.ChatMessageFileRepository;
import com.likelion.a1.chat.domain.repository.ChatMessageRepository;
import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import com.likelion.a1.media.application.port.out.MediaStoragePort;
import com.likelion.a1.media.application.port.out.StorageDownloadResult;
import com.likelion.a1.media.domain.model.AssetFile;
import com.likelion.a1.media.domain.model.GeneratedAsset;
import com.likelion.a1.media.domain.repository.AssetFileRepository;
import com.likelion.a1.media.domain.repository.GeneratedAssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class FileDownloadService {
  private final ChatService chatService;
  private final ChatMessageRepository messageRepository;
  private final ChatMessageFileRepository messageFileRepository;
  private final GeneratedAssetRepository generatedAssetRepository;
  private final AssetFileRepository assetFileRepository;
  private final MediaStoragePort mediaStoragePort;

  public FileDownloadService(
      ChatService chatService,
      ChatMessageRepository messageRepository,
      ChatMessageFileRepository messageFileRepository,
      GeneratedAssetRepository generatedAssetRepository,
      AssetFileRepository assetFileRepository,
      MediaStoragePort mediaStoragePort) {
    this.chatService = chatService;
    this.messageRepository = messageRepository;
    this.messageFileRepository = messageFileRepository;
    this.generatedAssetRepository = generatedAssetRepository;
    this.assetFileRepository = assetFileRepository;
    this.mediaStoragePort = mediaStoragePort;
  }

  public DownloadFile downloadChatMessageFile(Long userId, Long chatId, Long fileId) {
    chatService.findOwnedChat(userId, chatId);

    ChatMessageFile file =
        messageFileRepository
            .findById(fileId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_FILE_NOT_FOUND));

    ChatMessage message =
        messageRepository
            .findById(file.getMessageId())
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_FILE_NOT_FOUND));

    if (message.isDeleted() || !message.isInChat(chatId)) {
      throw new BusinessException(ErrorCode.CHAT_FILE_NOT_FOUND);
    }

    StorageDownloadResult result = mediaStoragePort.download(file.getBucketName(), file.getStoragePath());

    return new DownloadFile(
        result.content(),
        resolveContentType(result.contentType(), file.getMimeType()),
        resolveFilename(file.getOriginalFilename(), file.getStoredFilename()),
        resolveContentLength(result.contentLength(), file.getFileSize()));
  }

  public DownloadFile downloadGeneratedAssetFile(
      Long userId, Long chatId, Long generatedAssetId, Long fileId) {
    chatService.findOwnedChat(userId, chatId);

    GeneratedAsset asset =
        generatedAssetRepository
            .findById(generatedAssetId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ASSET_FILE_NOT_FOUND));

    if (asset.isDeleted() || !asset.isOwnedBy(userId) || !asset.getChatId().equals(chatId)) {
      throw new BusinessException(ErrorCode.ASSET_FILE_NOT_FOUND);
    }

    AssetFile file =
        assetFileRepository
            .findById(fileId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ASSET_FILE_NOT_FOUND));

    if (!file.getGeneratedAssetId().equals(generatedAssetId)) {
      throw new BusinessException(ErrorCode.ASSET_FILE_NOT_FOUND);
    }

    StorageDownloadResult result = mediaStoragePort.download(file.getBucketName(), file.getStoragePath());

    return new DownloadFile(
        result.content(),
        resolveContentType(result.contentType(), file.getMimeType()),
        resolveFilename(file.getOriginalFilename(), file.getStoredFilename()),
        resolveContentLength(result.contentLength(), file.getFileSize()));
  }

  private String resolveContentType(String storageContentType, String dbContentType) {
    if (StringUtils.hasText(storageContentType)) {
      return storageContentType;
    }

    if (StringUtils.hasText(dbContentType)) {
      return dbContentType;
    }

    return "application/octet-stream";
  }

  private String resolveFilename(String originalFilename, String storedFilename) {
    if (StringUtils.hasText(originalFilename)) {
      return originalFilename;
    }

    if (StringUtils.hasText(storedFilename)) {
      return storedFilename;
    }

    return "download";
  }

  private long resolveContentLength(Long storageContentLength, Long dbFileSize) {
    if (storageContentLength != null && storageContentLength >= 0) {
      return storageContentLength;
    }

    if (dbFileSize != null && dbFileSize >= 0) {
      return dbFileSize;
    }

    return 0;
  }

  public record DownloadFile(
      byte[] content, String contentType, String filename, long contentLength) {}
}
