package com.likelion.a1.notification.presentation.dto;

import java.time.OffsetDateTime;

public final class NotificationDtos {
  private NotificationDtos() {}

  public record Response(
      Long id,
      Long userId,
      String notificationType,
      String title,
      String content,
      String relatedType,
      Long relatedId,
      boolean read,
      OffsetDateTime readAt,
      OffsetDateTime createdAt) {}
}
