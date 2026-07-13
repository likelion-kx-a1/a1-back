package com.likelion.a1.chat.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "chats")
public class Chat {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long userId;

  private Long projectId;

  @Column(nullable = false)
  private String title;

  private Long firstMessageId;

  @Column(nullable = false)
  private boolean isGenerating;

  @Column(nullable = false, length = 30)
  private String status = "ACTIVE";

  @Column(nullable = false)
  private OffsetDateTime createdAt;

  @Column(nullable = false)
  private OffsetDateTime updatedAt;

  private OffsetDateTime deletedAt;

  public static Chat create(Long userId, Long projectId, String title) {
    Chat chat = new Chat();
    OffsetDateTime now = OffsetDateTime.now();

    chat.userId = userId;
    chat.projectId = projectId;
    chat.title = title;
    chat.isGenerating = false;
    chat.status = "ACTIVE";
    chat.createdAt = now;
    chat.updatedAt = now;

    return chat;
  }

  public void updateTitle(String title) {
    this.title = title;
    this.updatedAt = OffsetDateTime.now();
  }

  public void delete() {
    OffsetDateTime now = OffsetDateTime.now();

    this.status = "DELETED";
    this.deletedAt = now;
    this.updatedAt = now;
  }

  public void setFirstMessageIfAbsent(Long messageId) {
    if (this.firstMessageId == null) {
      this.firstMessageId = messageId;
      this.updatedAt = OffsetDateTime.now();
    }
  }

  public boolean isOwnedBy(Long userId) {
    return this.userId.equals(userId);
  }

  public boolean isInProject(Long projectId) {
    return this.projectId != null && this.projectId.equals(projectId);
  }

  public boolean isStandalone() {
    return this.projectId == null;
  }

  public boolean isDeleted() {
    return this.deletedAt != null || "DELETED".equals(this.status);
  }
}
