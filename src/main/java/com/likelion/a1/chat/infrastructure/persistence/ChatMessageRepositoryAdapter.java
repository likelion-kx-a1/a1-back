package com.likelion.a1.chat.infrastructure.persistence;

import com.likelion.a1.chat.domain.model.ChatMessage;
import com.likelion.a1.chat.domain.repository.ChatMessageRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ChatMessageRepositoryAdapter implements ChatMessageRepository {
  private final SpringDataChatMessageRepository repository;

  public ChatMessageRepositoryAdapter(SpringDataChatMessageRepository repository) {
    this.repository = repository;
  }

  @Override
  public ChatMessage save(ChatMessage message) {
    return repository.save(message);
  }

  @Override
  public Optional<ChatMessage> findById(Long id) {
    return repository.findById(id);
  }

  @Override
  public List<ChatMessage> findActiveByChatId(Long chatId) {
    return repository.findByChatIdAndStatusNotOrderBySortOrderAscCreatedAtAsc(chatId, "DELETED");
  }

  @Override
  public int countByChatId(Long chatId) {
    return repository.countByChatId(chatId);
  }
}
