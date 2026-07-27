package com.likelion.a1.notification.presentation.controller;

import com.likelion.a1.global.response.ApiResponse;
import com.likelion.a1.notification.application.service.NotificationService;
import com.likelion.a1.notification.presentation.dto.NotificationDtos.PageResponse;
import com.likelion.a1.notification.presentation.dto.NotificationDtos.Response;
import com.likelion.a1.notification.presentation.dto.NotificationDtos.UnreadCountResponse;
import com.likelion.a1.user.infrastructure.security.JwtPrincipal;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
  private final NotificationService notificationService;

  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping
  public ApiResponse<PageResponse> getNotifications(
      @AuthenticationPrincipal JwtPrincipal principal,
      @PageableDefault(size = 20) Pageable pageable) {
    return ApiResponse.success(
        "NOTIFICATIONS_FETCHED",
        "알림 목록을 조회했습니다.",
        notificationService.getNotifications(principal.userId(), pageable));
  }

  @GetMapping("/unread-count")
  public ApiResponse<UnreadCountResponse> getUnreadCount(
      @AuthenticationPrincipal JwtPrincipal principal) {
    return ApiResponse.success(
        "NOTIFICATION_UNREAD_COUNT_FETCHED",
        "읽지 않은 알림 개수를 조회했습니다.",
        notificationService.getUnreadCount(principal.userId()));
  }

  @PatchMapping("/{notificationId}/read")
  public ApiResponse<Response> markAsRead(
      @AuthenticationPrincipal JwtPrincipal principal,
      @PathVariable Long notificationId) {
    return ApiResponse.success(
        "NOTIFICATION_READ",
        "알림을 읽음 처리했습니다.",
        notificationService.markAsRead(principal.userId(), notificationId));
  }

  @PatchMapping("/read-all")
  public ApiResponse<Void> markAllAsRead(@AuthenticationPrincipal JwtPrincipal principal) {
    notificationService.markAllAsRead(principal.userId());
    return ApiResponse.success(
        "ALL_NOTIFICATIONS_READ", "모든 알림을 읽음 처리했습니다.", null);
  }
}
