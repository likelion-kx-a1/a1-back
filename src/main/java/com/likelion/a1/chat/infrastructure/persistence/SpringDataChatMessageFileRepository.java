package com.likelion.a1.chat.infrastructure.persistence;

import com.likelion.a1.chat.domain.model.ChatMessageFile;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataChatMessageFileRepository extends JpaRepository<ChatMessageFile, Long> {
  List<ChatMessageFile> findByMessageIdInOrderByIdAsc(Collection<Long> messageIds);
}
