package com.likelion.a1.chat.infrastructure.persistence;

import com.likelion.a1.chat.domain.model.ChatMessageFile;
import com.likelion.a1.chat.domain.repository.ChatMessageFileRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ChatMessageFileRepositoryAdapter implements ChatMessageFileRepository {
  private final SpringDataChatMessageFileRepository repository;

  public ChatMessageFileRepositoryAdapter(SpringDataChatMessageFileRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<ChatMessageFile> saveAll(List<ChatMessageFile> files) {
    return repository.saveAll(files);
  }

  @Override
  public List<ChatMessageFile> findByMessageIds(Collection<Long> messageIds) {
    if (messageIds == null || messageIds.isEmpty()) {
      return List.of();
    }

    return repository.findByMessageIdInOrderByIdAsc(messageIds);
  }

  @Override
  public Optional<ChatMessageFile> findById(Long id) {
    return repository.findById(id);
  }
}
