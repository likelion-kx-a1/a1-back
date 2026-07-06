package com.likelion.a1.chat.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "chat_messages")
public class ChatMessage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long chatId;

  private Long userId;

  @Column(nullable = false, length = 20)
  private String senderType;

  @Column(nullable = false, length = 20)
  private String messageType;

  @Column(columnDefinition = "text")
  private String contentText;

  private Long parentMessageId;
  private Long generationJobId;

  @Column(nullable = false)
  private int sortOrder;

  @Column(nullable = false, length = 30)
  private String status = "ACTIVE";

  @Column(nullable = false)
  private OffsetDateTime createdAt;

  @Column(nullable = false)
  private OffsetDateTime updatedAt;
}
