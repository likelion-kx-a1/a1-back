package com.likelion.a1.notification.application.listener;

import com.likelion.a1.generation.domain.event.GenerationCompletedEvent;
import com.likelion.a1.generation.domain.model.GenerationStatus;
import com.likelion.a1.notification.domain.model.Notification;
import com.likelion.a1.notification.infrastructure.persistence.NotificationRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class GenerationNotificationListener {
  private final NotificationRepository notificationRepository;

  public GenerationNotificationListener(NotificationRepository notificationRepository) {
    this.notificationRepository = notificationRepository;
  }

  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onGenerationCompleted(GenerationCompletedEvent event) {
    boolean succeeded = GenerationStatus.COMPLETED.name().equals(event.status());
    boolean video = event.jobType().toUpperCase().contains("VIDEO");
    String mediaLabel = video ? "영상" : "이미지";
    String type =
        succeeded
            ? (video ? "VIDEO_GENERATION_COMPLETED" : "IMAGE_GENERATION_COMPLETED")
            : (video ? "VIDEO_GENERATION_FAILED" : "IMAGE_GENERATION_FAILED");
    if (notificationRepository.existsByUserIdAndNotificationTypeAndRelatedTypeAndRelatedId(
        event.userId(), type, "GENERATION_JOB", event.jobId())) {
      return;
    }

    notificationRepository.save(
        Notification.create(
            event.userId(),
            type,
            succeeded ? mediaLabel + " 생성이 완료되었습니다." : mediaLabel + " 생성에 실패했습니다.",
            succeeded ? "생성된 에셋을 확인해 주세요." : "잠시 후 다시 요청해 주세요.",
            "GENERATION_JOB",
            event.jobId()));
  }
}
