package com.likelion.a1.chat.infrastructure.persistence;

import com.likelion.a1.chat.domain.model.ChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataChatMessageRepository extends JpaRepository<ChatMessage, Long> {
  List<ChatMessage> findByChatIdAndStatusNotOrderBySortOrderAscCreatedAtAsc(
      Long chatId, String status);

  int countByChatId(Long chatId);
}
