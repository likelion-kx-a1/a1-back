package com.likelion.a1.chat.application.service;

import com.likelion.a1.chat.domain.model.Chat;
import com.likelion.a1.chat.domain.model.ChatMessage;
import com.likelion.a1.chat.domain.model.ChatMessageFile;
import com.likelion.a1.chat.domain.repository.ChatMessageFileRepository;
import com.likelion.a1.chat.domain.repository.ChatMessageRepository;
import com.likelion.a1.chat.domain.repository.ChatRepository;
import com.likelion.a1.chat.presentation.dto.ChatDtos.ChatDetailResponse;
import com.likelion.a1.chat.presentation.dto.ChatDtos.ChatResponse;
import com.likelion.a1.chat.presentation.dto.ChatDtos.CreateChatRequest;
import com.likelion.a1.chat.presentation.dto.ChatDtos.MessageFileResponse;
import com.likelion.a1.chat.presentation.dto.ChatDtos.MessageResponse;
import com.likelion.a1.chat.presentation.dto.ChatDtos.UpdateChatRequest;
import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import com.likelion.a1.project.application.service.ProjectService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ChatService {
  private final ChatRepository chatRepository;
  private final ChatMessageRepository messageRepository;
  private final ChatMessageFileRepository fileRepository;
  private final ProjectService projectService;

  public ChatService(
      ChatRepository chatRepository,
      ChatMessageRepository messageRepository,
      ChatMessageFileRepository fileRepository,
      ProjectService projectService) {
    this.chatRepository = chatRepository;
    this.messageRepository = messageRepository;
    this.fileRepository = fileRepository;
    this.projectService = projectService;
  }

  public ChatResponse create(Long userId, CreateChatRequest request) {
    return create(
        userId, request.projectId(), request.title(), request.generationType(), request.imageCategory());
  }

  public ChatResponse createInProject(Long userId, Long projectId, CreateChatRequest request) {
    return create(userId, projectId, request.title(), request.generationType(), request.imageCategory());
  }

  @Transactional(readOnly = true)
  public List<ChatResponse> getChats(Long userId, Long projectId, Boolean outsideProject) {
    if (Boolean.TRUE.equals(outsideProject)) {
      return toResponseList(chatRepository.findActiveStandaloneByUserId(userId));
    }

    if (projectId != null) {
      projectService.findOwnedProject(userId, projectId);

      return toResponseList(chatRepository.findActiveByUserIdAndProjectId(userId, projectId));
    }

    return toResponseList(chatRepository.findActiveByUserId(userId));
  }

  @Transactional(readOnly = true)
  public ChatResponse getChat(Long userId, Long chatId) {
    return toResponse(findOwnedChat(userId, chatId));
  }

  @Transactional(readOnly = true)
  public ChatDetailResponse getChatDetail(Long userId, Long chatId) {
    Chat chat = findOwnedChat(userId, chatId);
    List<ChatMessage> messages = messageRepository.findActiveByChatId(chatId);
    Map<Long, List<ChatMessageFile>> filesByMessageId =
        fileRepository.findByMessageIds(messages.stream().map(ChatMessage::getId).toList()).stream()
            .collect(Collectors.groupingBy(ChatMessageFile::getMessageId));

    return new ChatDetailResponse(
        chat.getId(),
        chat.getProjectId(),
        chat.getTitle(),
        chat.getGenerationType(),
        chat.getImageCategory(),
        chat.getFirstMessageId(),
        chat.isGenerating(),
        chat.getStatus(),
        messages.stream()
            .map(message -> toMessageResponse(message, filesByMessageId.getOrDefault(message.getId(), List.of())))
            .toList(),
        chat.getCreatedAt(),
        chat.getUpdatedAt());
  }

  public ChatResponse update(Long userId, Long chatId, UpdateChatRequest request) {
    Chat chat = findOwnedChat(userId, chatId);
    chat.updateTitle(request.title().trim());

    return toResponse(chatRepository.save(chat));
  }

  public void delete(Long userId, Long chatId) {
    Chat chat = findOwnedChat(userId, chatId);
    chat.delete();

    chatRepository.save(chat);
  }

  public ChatResponse startGenerating(Long userId, Long chatId) {
    Chat chat = findOwnedChat(userId, chatId);
    chat.startGenerating();

    return toResponse(chatRepository.save(chat));
  }

  public ChatResponse finishGenerating(Long userId, Long chatId) {
    Chat chat = findOwnedChat(userId, chatId);
    chat.finishGenerating();

    return toResponse(chatRepository.save(chat));
  }

  public Chat findOwnedChat(Long userId, Long chatId) {
    Chat chat =
        chatRepository
            .findById(chatId)
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));

    if (chat.isDeleted() || !chat.isOwnedBy(userId)) {
      throw new BusinessException(ErrorCode.INVALID_INPUT);
    }

    return chat;
  }

  private ChatResponse create(
      Long userId, Long projectId, String title, String generationType, String imageCategory) {
    if (projectId != null) {
      projectService.findOwnedProject(userId, projectId);
    }

    Chat chat =
        Chat.create(
            userId,
            projectId,
            title.trim(),
            normalizeGenerationType(generationType),
            normalizeNullable(imageCategory));

    return toResponse(chatRepository.save(chat));
  }

  private String normalizeGenerationType(String generationType) {
    if (generationType == null || generationType.isBlank()) {
      return "IMAGE";
    }

    return generationType.trim().toUpperCase();
  }

  private String normalizeNullable(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }

    return value.trim();
  }

  private List<ChatResponse> toResponseList(List<Chat> chats) {
    return chats.stream().map(this::toResponse).toList();
  }

  private ChatResponse toResponse(Chat chat) {
    return new ChatResponse(
        chat.getId(),
        chat.getProjectId(),
        chat.getTitle(),
        chat.getGenerationType(),
        chat.getImageCategory(),
        chat.getFirstMessageId(),
        chat.isGenerating(),
        chat.getStatus(),
        chat.getCreatedAt(),
        chat.getUpdatedAt());
  }

  private MessageResponse toMessageResponse(ChatMessage message, List<ChatMessageFile> files) {
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
