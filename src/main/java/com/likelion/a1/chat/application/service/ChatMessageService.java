package com.likelion.a1.chat.application.service;

import com.likelion.a1.chat.domain.model.Chat;
import com.likelion.a1.chat.domain.model.ChatMessage;
import com.likelion.a1.chat.domain.model.ChatMessageFile;
import com.likelion.a1.chat.domain.repository.ChatMessageFileRepository;
import com.likelion.a1.chat.domain.repository.ChatMessageRepository;
import com.likelion.a1.chat.domain.repository.ChatRepository;
import com.likelion.a1.chat.presentation.dto.ChatDtos.CreateMessageRequest;
import com.likelion.a1.chat.presentation.dto.ChatDtos.MessageFileRequest;
import com.likelion.a1.chat.presentation.dto.ChatDtos.MessageFileResponse;
import com.likelion.a1.chat.presentation.dto.ChatDtos.MessageResponse;
import com.likelion.a1.chat.presentation.dto.ChatDtos.UpdateMessageRequest;
import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ChatMessageService {
  private final ChatService chatService;
  private final ChatRepository chatRepository;
  private final ChatMessageRepository messageRepository;
  private final ChatMessageFileRepository fileRepository;

  public ChatMessageService(
      ChatService chatService,
      ChatRepository chatRepository,
      ChatMessageRepository messageRepository,
      ChatMessageFileRepository fileRepository) {
    this.chatService = chatService;
    this.chatRepository = chatRepository;
    this.messageRepository = messageRepository;
    this.fileRepository = fileRepository;
  }

  public MessageResponse create(Long userId, Long chatId, CreateMessageRequest request) {
    Chat chat = chatService.findOwnedChat(userId, chatId);
    int sortOrder = messageRepository.countByChatId(chatId) + 1;

    String messageType = normalizeMessageType(request.messageType(), request.contentText(), request.files());
    String generationType = normalizeNullableUpper(request.generationType());
    String imageCategory = normalizeNullableUpper(request.imageCategory());
    String contentText = normalizeNullable(request.contentText());

    validateMessageRequest(messageType, contentText, generationType, imageCategory, request.files());

    ChatMessage message =
        ChatMessage.create(
            chatId,
            userId,
            normalizeSenderType(request.senderType()),
            messageType,
            contentText,
            generationType,
            imageCategory,
            request.parentMessageId(),
            sortOrder);

    ChatMessage savedMessage = messageRepository.save(message);
    List<ChatMessageFile> savedFiles = saveFiles(savedMessage.getId(), request.files());

    chat.setFirstMessageIfAbsent(savedMessage.getId());
    chatRepository.save(chat);

    return toResponse(savedMessage, savedFiles);
  }

  @Transactional(readOnly = true)
  public List<MessageResponse> getMessages(Long userId, Long chatId) {
    chatService.findOwnedChat(userId, chatId);

    return getMessagesForOwnedChat(chatId);
  }

  @Transactional(readOnly = true)
  public List<MessageResponse> getMessagesForOwnedChat(Long chatId) {
    List<ChatMessage> messages = messageRepository.findActiveByChatId(chatId);
    Map<Long, List<ChatMessageFile>> filesByMessageId =
        fileRepository.findByMessageIds(messages.stream().map(ChatMessage::getId).toList()).stream()
            .collect(Collectors.groupingBy(ChatMessageFile::getMessageId));

    return messages.stream()
        .map(message -> toResponse(message, filesByMessageId.getOrDefault(message.getId(), List.of())))
        .toList();
  }

  public MessageResponse update(Long userId, Long chatId, Long messageId, UpdateMessageRequest request) {
    chatService.findOwnedChat(userId, chatId);
    ChatMessage message = findMessageInChat(chatId, messageId);
    message.updateContent(request.contentText().trim());

    ChatMessage savedMessage = messageRepository.save(message);
    List<ChatMessageFile> files = fileRepository.findByMessageIds(List.of(savedMessage.getId()));

    return toResponse(savedMessage, files);
  }

  public void delete(Long userId, Long chatId, Long messageId) {
    chatService.findOwnedChat(userId, chatId);
    ChatMessage message = findMessageInChat(chatId, messageId);
    message.delete();

    messageRepository.save(message);
  }

  /**
   * {@link com.likelion.a1.generation.application.service.GenerationResultService}가 방금 만든
   * 어시스턴트 메시지를, 그 결과를 만들어낸 GenerationJob/GeneratedAsset과 연결한다. 채팅방 소유권은
   * 이미 그 메시지를 만드는 과정에서 확인됐으므로 여기서 다시 검증하지 않는다.
   */
  public MessageResponse attachGenerationResult(
      Long messageId, Long generationJobId, Long generatedAssetId) {
    ChatMessage message =
        messageRepository
            .findById(messageId)
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));
    message.associateGenerationResult(generationJobId, generatedAssetId);

    ChatMessage savedMessage = messageRepository.save(message);
    List<ChatMessageFile> files = fileRepository.findByMessageIds(List.of(savedMessage.getId()));

    return toResponse(savedMessage, files);
  }

  private ChatMessage findMessageInChat(Long chatId, Long messageId) {
    ChatMessage message =
        messageRepository
            .findById(messageId)
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));

    if (message.isDeleted() || !message.isInChat(chatId)) {
      throw new BusinessException(ErrorCode.INVALID_INPUT);
    }

    return message;
  }

  private List<ChatMessageFile> saveFiles(Long messageId, List<MessageFileRequest> fileRequests) {
    if (fileRequests == null || fileRequests.isEmpty()) {
      return List.of();
    }

    List<ChatMessageFile> files =
        fileRequests.stream()
            .map(
                file ->
                    ChatMessageFile.create(
                        messageId,
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
                        file.durationSeconds()))
            .toList();

    return fileRepository.saveAll(files);
  }

  private void validateMessageRequest(
      String messageType,
      String contentText,
      String generationType,
      String imageCategory,
      List<MessageFileRequest> files) {
    boolean hasText = StringUtils.hasText(contentText);
    boolean hasFiles = files != null && !files.isEmpty();

    if (!hasText && !hasFiles) {
      throw new BusinessException(ErrorCode.INVALID_INPUT);
    }

    if (requiresGenerationType(messageType) && !StringUtils.hasText(generationType)) {
      throw new BusinessException(ErrorCode.INVALID_INPUT);
    }

    if ("IMAGE".equals(generationType)
        && StringUtils.hasText(imageCategory)
        && !List.of("CHARACTER", "BACKGROUND", "ETC").contains(imageCategory)) {
      throw new BusinessException(ErrorCode.INVALID_INPUT);
    }

    if ("VIDEO".equals(generationType) && StringUtils.hasText(imageCategory)) {
      throw new BusinessException(ErrorCode.INVALID_INPUT);
    }

    if ("TEXT".equals(messageType) && !hasText) {
      throw new BusinessException(ErrorCode.INVALID_INPUT);
    }

    if ("FILE".equals(messageType) && !hasFiles) {
      throw new BusinessException(ErrorCode.INVALID_INPUT);
    }
  }

  private boolean requiresGenerationType(String messageType) {
    return "IMAGE".equals(messageType)
        || "VIDEO".equals(messageType)
        || "ASSET".equals(messageType)
        || "ASSET_REQUEST".equals(messageType);
  }

  private String normalizeSenderType(String senderType) {
    if (!StringUtils.hasText(senderType)) {
      return "USER";
    }

    String normalized = senderType.trim().toUpperCase();
    return "AI".equals(normalized) ? "ASSISTANT" : normalized;
  }

  private String normalizeMessageType(
      String messageType, String contentText, Collection<MessageFileRequest> files) {
    if (StringUtils.hasText(messageType)) {
      return messageType.trim().toUpperCase();
    }

    boolean hasText = StringUtils.hasText(contentText);
    boolean hasFiles = files != null && !files.isEmpty();

    if (hasText && hasFiles) {
      return "MIXED";
    }

    if (hasFiles) {
      return "FILE";
    }

    return "TEXT";
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

  private MessageResponse toResponse(ChatMessage message, List<ChatMessageFile> files) {
    return new MessageResponse(
        message.getId(),
        message.getChatId(),
        message.getSenderType(),
        message.getMessageType(),
        message.getContentText(),
        message.getGenerationType(),
        message.getImageCategory(),
        message.getParentMessageId(),
        message.getGenerationJobId(),
        message.getGeneratedAssetId(),
        message.getSortOrder(),
        message.getStatus(),
        files.stream().map(this::toFileResponse).toList(),
        message.getCreatedAt(),
        message.getUpdatedAt());
  }

  private MessageFileResponse toFileResponse(ChatMessageFile file) {
    return new MessageFileResponse(
        file.getId(),
        file.getMessageId(),
        file.getFileType(),
        file.getBucketName(),
        file.getStoragePath(),
        file.getPublicUrl(),
        file.getOriginalFilename(),
        file.getStoredFilename(),
        file.getMimeType(),
        file.getFileSize(),
        file.getWidth(),
        file.getHeight(),
        file.getDurationSeconds(),
        file.getCreatedAt());
  }
}
