package com.likelion.a1.chat.domain.repository;

import com.likelion.a1.chat.domain.model.Chat;
import java.util.List;
import java.util.Optional;

public interface ChatRepository {
  Chat save(Chat chat);

  Optional<Chat> findById(Long id);

  List<Chat> findActiveByUserId(Long userId);

  List<Chat> findActiveByUserIdAndProjectId(Long userId, Long projectId);

  List<Chat> findActiveStandaloneByUserId(Long userId);

  Optional<Chat> findFirstActiveByUserIdAndProjectId(Long userId, Long projectId);
}
