package com.likelion.a1.chat.domain.repository;

import com.likelion.a1.chat.domain.model.ChatMessage;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository {
  ChatMessage save(ChatMessage message);

  Optional<ChatMessage> findById(Long id);

  List<ChatMessage> findActiveByChatId(Long chatId);

  int countByChatId(Long chatId);
}
