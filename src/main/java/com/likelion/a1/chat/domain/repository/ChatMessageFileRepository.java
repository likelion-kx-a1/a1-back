package com.likelion.a1.chat.domain.repository;

import com.likelion.a1.chat.domain.model.ChatMessageFile;
import java.util.Collection;
import java.util.List;

public interface ChatMessageFileRepository {
  List<ChatMessageFile> saveAll(List<ChatMessageFile> files);

  List<ChatMessageFile> findByMessageIds(Collection<Long> messageIds);
}
