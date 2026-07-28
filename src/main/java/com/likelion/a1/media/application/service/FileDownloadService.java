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
import com.likelion.a1.media.domain.model.GeneratedAsset;
import com.likelion.a1.media.domain.model.SavedAsset;
import com.likelion.a1.media.domain.model.SavedAssetFile;
import com.likelion.a1.media.domain.repository.GeneratedAssetRepository;
import com.likelion.a1.media.domain.repository.SavedAssetFileRepository;
import com.likelion.a1.media.domain.repository.SavedAssetRepository;
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
  private final SavedAssetRepository savedAssetRepository;
  private final SavedAssetFileRepository savedAssetFileRepository;
  private final MediaStoragePort mediaStoragePort;

  public FileDownloadService(
      ChatService chatService,
      ChatMessageRepository messageRepository,
      ChatMessageFileRepository messageFileRepository,
      GeneratedAssetRepository generatedAssetRepository,
      SavedAssetRepository savedAssetRepository,
      SavedAssetFileRepository savedAssetFileRepository,
      MediaStoragePort mediaStoragePort) {
    this.chatService = chatService;
    this.messageRepository = messageRepository;
    this.messageFileRepository = messageFileRepository;
    this.generatedAssetRepository = generatedAssetRepository;
    this.savedAssetRepository = savedAssetRepository;
    this.savedAssetFileRepository = savedAssetFileRepository;
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

  /**
   * fileId는 AssetFile.id가 아니라 {@code GET /api/chats/{chatId}/messages}가 실제로 내려주는
   * ChatMessageFile.id다. AssetFile은 같은 업로드 결과로부터 별도 테이블에 독립된 id로 함께 생성되지만
   * 그 id를 노출하는 응답이 어디에도 없어(docs_h/제노바_KX_생성결과.md #1), AssetFile.id로 조회하던
   * 기존 구현은 클라이언트가 절대 맞출 수 없는 값을 기대해 항상 404가 났다. 그래서 ChatMessageFile을
   * 조회 기준으로 삼고, 그 파일이 속한 메시지의 generatedAssetId가 경로의 generatedAssetId와 실제로
   * 일치하는지 검증한다.
   */
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

    ChatMessageFile file =
        messageFileRepository
            .findById(fileId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ASSET_FILE_NOT_FOUND));

    ChatMessage message =
        messageRepository
            .findById(file.getMessageId())
            .orElseThrow(() -> new BusinessException(ErrorCode.ASSET_FILE_NOT_FOUND));

    if (message.isDeleted()
        || !message.isInChat(chatId)
        || !generatedAssetId.equals(message.getGeneratedAssetId())) {
      throw new BusinessException(ErrorCode.ASSET_FILE_NOT_FOUND);
    }

    StorageDownloadResult result = mediaStoragePort.download(file.getBucketName(), file.getStoragePath());

    return new DownloadFile(
        result.content(),
        resolveContentType(result.contentType(), file.getMimeType()),
        resolveFilename(file.getOriginalFilename(), file.getStoredFilename()),
        resolveContentLength(result.contentLength(), file.getFileSize()));
  }

  public DownloadFile downloadSavedAssetFile(Long userId, Long savedAssetId, Long fileId) {
    SavedAsset asset =
        savedAssetRepository
            .findById(savedAssetId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ASSET_FILE_NOT_FOUND));

    if (asset.isDeleted() || !asset.isOwnedBy(userId)) {
      throw new BusinessException(ErrorCode.ASSET_FILE_NOT_FOUND);
    }

    SavedAssetFile file =
        savedAssetFileRepository.findBySavedAssetId(savedAssetId).stream()
            .filter(candidate -> candidate.getId().equals(fileId))
            .findFirst()
            .orElseThrow(() -> new BusinessException(ErrorCode.ASSET_FILE_NOT_FOUND));

    StorageDownloadResult result =
        mediaStoragePort.download(file.getBucketName(), file.getStoragePath());

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
