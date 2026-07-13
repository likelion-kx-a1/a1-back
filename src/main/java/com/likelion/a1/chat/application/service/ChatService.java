package com.likelion.a1.chat.application.service;

import com.likelion.a1.chat.domain.model.Chat;
import com.likelion.a1.chat.domain.repository.ChatRepository;
import com.likelion.a1.chat.presentation.dto.ChatDtos.ChatResponse;
import com.likelion.a1.chat.presentation.dto.ChatDtos.CreateChatRequest;
import com.likelion.a1.chat.presentation.dto.ChatDtos.UpdateChatRequest;
import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import com.likelion.a1.project.application.service.ProjectService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ChatService {
  private final ChatRepository chatRepository;
  private final ProjectService projectService;

  public ChatService(ChatRepository chatRepository, ProjectService projectService) {
    this.chatRepository = chatRepository;
    this.projectService = projectService;
  }

  public ChatResponse create(Long userId, CreateChatRequest request) {
    return create(userId, request.projectId(), request.title());
  }

  public ChatResponse createInProject(Long userId, Long projectId, CreateChatRequest request) {
    return create(userId, projectId, request.title());
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

  private ChatResponse create(Long userId, Long projectId, String title) {
    if (projectId != null) {
      projectService.findOwnedProject(userId, projectId);
    }

    Chat chat = Chat.create(userId, projectId, title.trim());

    return toResponse(chatRepository.save(chat));
  }

  private List<ChatResponse> toResponseList(List<Chat> chats) {
    return chats.stream().map(this::toResponse).toList();
  }

  private ChatResponse toResponse(Chat chat) {
    return new ChatResponse(
        chat.getId(),
        chat.getProjectId(),
        chat.getTitle(),
        chat.getFirstMessageId(),
        chat.isGenerating(),
        chat.getStatus(),
        chat.getCreatedAt(),
        chat.getUpdatedAt());
  }
}
