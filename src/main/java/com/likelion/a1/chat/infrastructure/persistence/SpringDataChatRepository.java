package com.likelion.a1.chat.infrastructure.persistence;

import com.likelion.a1.chat.domain.model.Chat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataChatRepository extends JpaRepository<Chat, Long> {
  List<Chat> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);

  List<Chat> findByUserIdAndProjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(
      Long userId, Long projectId);

  List<Chat> findByUserIdAndProjectIdIsNullAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);
}
