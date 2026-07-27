package com.likelion.a1.notification.infrastructure.persistence;

import com.likelion.a1.notification.domain.model.Notification;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
  Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

  Optional<Notification> findByIdAndUserId(Long id, Long userId);

  long countByUserIdAndIsReadFalse(Long userId);

  boolean existsByUserIdAndNotificationTypeAndRelatedTypeAndRelatedId(
      Long userId, String notificationType, String relatedType, Long relatedId);
}
