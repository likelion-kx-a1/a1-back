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
}
