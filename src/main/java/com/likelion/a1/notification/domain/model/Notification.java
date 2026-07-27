package com.likelion.a1.notification.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "notifications")
public class Notification {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long userId;

  @Column(nullable = false, length = 50)
  private String notificationType;

  @Column(nullable = false)
  private String title;

  @Column(columnDefinition = "text")
  private String content;

  @Column(length = 50)
  private String relatedType;

  private Long relatedId;

  @Column(nullable = false)
  private boolean isRead;

  private OffsetDateTime readAt;

  @Column(nullable = false)
  private OffsetDateTime createdAt;

  public static Notification create(
      Long userId,
      String notificationType,
      String title,
      String content,
      String relatedType,
      Long relatedId) {
    Notification notification = new Notification();
    notification.userId = userId;
    notification.notificationType = notificationType;
    notification.title = title;
    notification.content = content;
    notification.relatedType = relatedType;
    notification.relatedId = relatedId;
    notification.isRead = false;
    notification.createdAt = OffsetDateTime.now();
    return notification;
  }

  public void markAsRead() {
    if (isRead) return;
    isRead = true;
    readAt = OffsetDateTime.now();
  }
}
