package com.likelion.a1.library.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "library_messages")
public class LibraryMessage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long libraryId;

  private Long userId;
  private String senderType;

  private String messageType;

  @Column(columnDefinition = "text")
  private String contentText;

  private Long parentMessageId;

  private Long generationJobId;

  @Column(nullable = false)
  private int sortOrder;

  @Column(nullable = false)
  private String status;

  @Column(nullable = false)
  private OffsetDateTime createdAt;

  @Column(nullable = false)
  private OffsetDateTime updatedAt;
}
