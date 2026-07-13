package com.likelion.a1.chat.application.service;

import com.likelion.a1.chat.domain.model.Chat;
import com.likelion.a1.chat.domain.model.ChatMessage;
import com.likelion.a1.chat.domain.repository.ChatMessageRepository;
import com.likelion.a1.chat.domain.repository.ChatRepository;
import com.likelion.a1.chat.presentation.dto.ChatDtos.CreateMessageRequest;
import com.likelion.a1.chat.presentation.dto.ChatDtos.MessageResponse;
import com.likelion.a1.chat.presentation.dto.ChatDtos.UpdateMessageRequest;
import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ChatMessageService {
  private final ChatService chatService;
  private final ChatRepository chatRepository;
  private final ChatMessageRepository messageRepository;

  public ChatMessageService(
      ChatService chatService, ChatRepository chatRepository, ChatMessageRepository messageRepository) {
    this.chatService = chatService;
    this.chatRepository = chatRepository;
    this.messageRepository = messageRepository;
  }

  public MessageResponse create(Long userId, Long chatId, CreateMessageRequest request) {
    Chat chat = chatService.findOwnedChat(userId, chatId);
    int sortOrder = messageRepository.countByChatId(chatId) + 1;

    ChatMessage message =
        ChatMessage.create(
            chatId,
            userId,
            normalizeSenderType(request.senderType()),
            normalizeMessageType(request.messageType()),
            request.contentText().trim(),
            request.parentMessageId(),
            sortOrder);

    ChatMessage savedMessage = messageRepository.save(message);
    chat.setFirstMessageIfAbsent(savedMessage.getId());
    chatRepository.save(chat);

    return toResponse(savedMessage);
  }

  @Transactional(readOnly = true)
  public List<MessageResponse> getMessages(Long userId, Long chatId) {
    chatService.findOwnedChat(userId, chatId);

    return messageRepository.findActiveByChatId(chatId).stream().map(this::toResponse).toList();
  }

  public MessageResponse update(Long userId, Long chatId, Long messageId, UpdateMessageRequest request) {
    chatService.findOwnedChat(userId, chatId);
    ChatMessage message = findMessageInChat(chatId, messageId);
    message.updateContent(request.contentText().trim());

    return toResponse(messageRepository.save(message));
  }

  public void delete(Long userId, Long chatId, Long messageId) {
    chatService.findOwnedChat(userId, chatId);
    ChatMessage message = findMessageInChat(chatId, messageId);
    message.delete();

    messageRepository.save(message);
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

  private String normalizeSenderType(String senderType) {
    if (!StringUtils.hasText(senderType)) {
      return "USER";
    }

    return senderType.trim().toUpperCase();
  }

  private String normalizeMessageType(String messageType) {
    if (!StringUtils.hasText(messageType)) {
      return "TEXT";
    }

    return messageType.trim().toUpperCase();
  }

  private MessageResponse toResponse(ChatMessage message) {
    return new MessageResponse(
        message.getId(),
        message.getChatId(),
        message.getSenderType(),
        message.getMessageType(),
        message.getContentText(),
        message.getParentMessageId(),
        message.getGenerationJobId(),
        message.getGeneratedAssetId(),
        message.getSortOrder(),
        message.getStatus(),
        message.getCreatedAt(),
        message.getUpdatedAt());
  }
}
