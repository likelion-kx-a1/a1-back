package com.likelion.a1.generation.application.service;

import com.likelion.a1.chat.application.service.ChatMessageService;
import com.likelion.a1.chat.application.service.ChatService;
import com.likelion.a1.chat.presentation.dto.ChatDtos.CreateMessageRequest;
import com.likelion.a1.chat.presentation.dto.ChatDtos.MessageFileRequest;
import com.likelion.a1.chat.presentation.dto.ChatDtos.MessageResponse;
import com.likelion.a1.generation.presentation.dto.GenerationResultDtos.AssistantResultResponse;
import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import com.likelion.a1.media.application.service.FileUploadService;
import com.likelion.a1.media.domain.model.AssetFile;
import com.likelion.a1.media.domain.model.GeneratedAsset;
import com.likelion.a1.media.domain.repository.AssetFileRepository;
import com.likelion.a1.media.domain.repository.GeneratedAssetRepository;
import com.likelion.a1.media.presentation.dto.FileUploadDtos.UploadResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class GenerationResultService {
  private final ChatService chatService;
  private final ChatMessageService chatMessageService;
  private final FileUploadService fileUploadService;
  private final GeneratedAssetRepository generatedAssetRepository;
  private final AssetFileRepository assetFileRepository;

  public GenerationResultService(
      ChatService chatService,
      ChatMessageService chatMessageService,
      FileUploadService fileUploadService,
      GeneratedAssetRepository generatedAssetRepository,
      AssetFileRepository assetFileRepository) {
    this.chatService = chatService;
    this.chatMessageService = chatMessageService;
    this.fileUploadService = fileUploadService;
    this.generatedAssetRepository = generatedAssetRepository;
    this.assetFileRepository = assetFileRepository;
  }

  public MessageFileRequest uploadGeneratedFile(
      Long userId,
      Long chatId,
      byte[] content,
      String originalFilename,
      String contentType,
      String fileType) {
    UploadResponse uploadedFile =
        fileUploadService.uploadGeneratedChatFile(
            userId, chatId, content, originalFilename, contentType, fileType);

    return new MessageFileRequest(
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
        null);
  }

  public AssistantResultResponse saveAssistantTextResult(
      Long userId,
      Long chatId,
      Long parentMessageId,
      String contentText,
      String generationType,
      String imageCategory) {
    MessageResponse message =
        chatMessageService.create(
            userId,
            chatId,
            new CreateMessageRequest(
                "ASSISTANT",
                "TEXT",
                contentText,
                generationType,
                imageCategory,
                parentMessageId,
                List.of()));

    chatService.finishGenerating(userId, chatId);

    return new AssistantResultResponse(message, null);
  }

  public AssistantResultResponse saveAssistantAssetResult(
      Long userId,
      Long chatId,
      Long parentMessageId,
      Long generationJobId,
      Long parentAssetId,
      String contentText,
      String generationType,
      String imageCategory,
      String title,
      String prompt,
      List<MessageFileRequest> files) {
    if (files == null || files.isEmpty()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT);
    }

    String normalizedAssetType = normalizeRequiredUpper(generationType);
    MessageResponse message =
        chatMessageService.create(
            userId,
            chatId,
            new CreateMessageRequest(
                "ASSISTANT",
                "ASSET",
                contentText,
                normalizedAssetType,
                normalizeNullableUpper(imageCategory),
                parentMessageId,
                files));

    GeneratedAsset asset =
        generatedAssetRepository.save(
            GeneratedAsset.create(
                userId,
                chatId,
                generationJobId,
                message.messageId(),
                parentAssetId,
                normalizedAssetType,
                normalizeNullableUpper(imageCategory),
                normalizeNullable(title),
                normalizeNullable(prompt)));

    assetFileRepository.saveAll(
        files.stream()
            .map(file -> toAssetFile(asset.getId(), file))
            .toList());

    chatService.finishGenerating(userId, chatId);

    return new AssistantResultResponse(message, asset.getId());
  }

  public void startGenerating(Long userId, Long chatId) {
    chatService.startGenerating(userId, chatId);
  }

  public void finishGenerating(Long userId, Long chatId) {
    chatService.finishGenerating(userId, chatId);
  }

  private AssetFile toAssetFile(Long generatedAssetId, MessageFileRequest file) {
    return AssetFile.create(
        generatedAssetId,
        normalizeNullableUpper(file.fileType()),
        file.bucketName().trim(),
        file.storagePath().trim(),
        normalizeNullable(file.publicUrl()),
        normalizeNullable(file.originalFilename()),
        normalizeNullable(file.storedFilename()),
        normalizeNullable(file.mimeType()),
        file.fileSize(),
        file.width(),
        file.height(),
        file.durationSeconds());
  }

  private String normalizeRequiredUpper(String value) {
    if (!StringUtils.hasText(value)) {
      throw new BusinessException(ErrorCode.INVALID_INPUT);
    }

    String normalized = value.trim().toUpperCase();
    if (!List.of("IMAGE", "VIDEO").contains(normalized)) {
      throw new BusinessException(ErrorCode.INVALID_INPUT);
    }

    return normalized;
  }

  private String normalizeNullableUpper(String value) {
    String normalized = normalizeNullable(value);
    return normalized == null ? null : normalized.toUpperCase();
  }

  private String normalizeNullable(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }

    return value.trim();
  }
}
