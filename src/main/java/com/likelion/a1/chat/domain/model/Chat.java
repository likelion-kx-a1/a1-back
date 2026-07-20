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

  @Column(nullable = false, length = 20)
  private String generationType;

  @Column(length = 30)
  private String imageCategory;

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

  public static Chat create(
      Long userId, Long projectId, String title, String generationType, String imageCategory) {
    Chat chat = new Chat();
    OffsetDateTime now = OffsetDateTime.now();

    chat.userId = userId;
    chat.projectId = projectId;
    chat.title = title;
    chat.generationType = generationType;
    chat.imageCategory = imageCategory;
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

    // Hard delete는 하지 않는다. 채팅 내용/파일 참조 보존을 위해 비활성 상태로만 전환한다.
    // 이전 정책은 "DELETED"였지만, 채팅 삭제는 status=INACTIVE 소프트 삭제로 통일한다.
    this.status = "INACTIVE";
    this.deletedAt = now;
    this.updatedAt = now;
  }

  public void setFirstMessageIfAbsent(Long messageId) {
    if (this.firstMessageId == null) {
      this.firstMessageId = messageId;
      this.updatedAt = OffsetDateTime.now();
    }
  }

  public void startGenerating() {
    this.isGenerating = true;
    this.updatedAt = OffsetDateTime.now();
  }

  public void finishGenerating() {
    this.isGenerating = false;
    this.updatedAt = OffsetDateTime.now();
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
    return this.deletedAt != null || "INACTIVE".equals(this.status) || "DELETED".equals(this.status);
  }
}
