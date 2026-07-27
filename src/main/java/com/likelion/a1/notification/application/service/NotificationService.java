package com.likelion.a1.notification.application.service;

import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import com.likelion.a1.notification.domain.model.Notification;
import com.likelion.a1.notification.infrastructure.persistence.NotificationRepository;
import com.likelion.a1.notification.presentation.dto.NotificationDtos.PageResponse;
import com.likelion.a1.notification.presentation.dto.NotificationDtos.Response;
import com.likelion.a1.notification.presentation.dto.NotificationDtos.UnreadCountResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NotificationService {
  private final NotificationRepository notificationRepository;

  public NotificationService(NotificationRepository notificationRepository) {
    this.notificationRepository = notificationRepository;
  }

  public PageResponse getNotifications(Long userId, Pageable pageable) {
    Page<Notification> page =
        notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    List<Response> content = page.getContent().stream().map(this::toResponse).toList();
    return new PageResponse(
        content,
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.isLast());
  }

  public UnreadCountResponse getUnreadCount(Long userId) {
    return new UnreadCountResponse(notificationRepository.countByUserIdAndIsReadFalse(userId));
  }

  @Transactional
  public Response markAsRead(Long userId, Long notificationId) {
    Notification notification =
        notificationRepository
            .findByIdAndUserId(notificationId, userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
    notification.markAsRead();
    return toResponse(notificationRepository.save(notification));
  }

  @Transactional
  public void markAllAsRead(Long userId) {
    notificationRepository
        .findByUserIdOrderByCreatedAtDesc(userId, Pageable.unpaged())
        .forEach(Notification::markAsRead);
  }

  private Response toResponse(Notification notification) {
    return new Response(
        notification.getId(),
        notification.getUserId(),
        notification.getNotificationType(),
        notification.getTitle(),
        notification.getContent(),
        notification.getRelatedType(),
        notification.getRelatedId(),
        notification.isRead(),
        notification.getReadAt(),
        notification.getCreatedAt());
  }
}
