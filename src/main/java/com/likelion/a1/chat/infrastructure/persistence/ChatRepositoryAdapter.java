package com.likelion.a1.chat.infrastructure.persistence;

import com.likelion.a1.chat.domain.model.Chat;
import com.likelion.a1.chat.domain.repository.ChatRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ChatRepositoryAdapter implements ChatRepository {
  private final SpringDataChatRepository repository;

  public ChatRepositoryAdapter(SpringDataChatRepository repository) {
    this.repository = repository;
  }

  @Override
  public Chat save(Chat chat) {
    return repository.save(chat);
  }

  @Override
  public Optional<Chat> findById(Long id) {
    return repository.findById(id);
  }

  @Override
  public List<Chat> findActiveByUserId(Long userId) {
    return repository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
  }

  @Override
  public List<Chat> findActiveByUserIdAndProjectId(Long userId, Long projectId) {
    return repository.findByUserIdAndProjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(
        userId, projectId);
  }

  @Override
  public List<Chat> findActiveStandaloneByUserId(Long userId) {
    return repository.findByUserIdAndProjectIdIsNullAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
  }
}
