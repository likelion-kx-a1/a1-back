package com.likelion.a1.notification.application.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.likelion.a1.generation.domain.event.GenerationCompletedEvent;
import com.likelion.a1.notification.domain.model.Notification;
import com.likelion.a1.notification.infrastructure.persistence.NotificationRepository;
import com.likelion.a1.sse.presentation.dto.SseDtos.GenerationCompletedPayload;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GenerationNotificationListenerTest {

  @Test
  void reversePromptCompletionCreatesDedicatedServerAndSseNotifications() {
    NotificationRepository repository = mock(NotificationRepository.class);
    when(repository.existsByUserIdAndNotificationTypeAndRelatedTypeAndRelatedId(
            any(), any(), any(), any()))
        .thenReturn(false);
    GenerationNotificationListener listener = new GenerationNotificationListener(repository);
    GenerationCompletedEvent event =
        new GenerationCompletedEvent(
            7L, 12L, 30L, "REVERSE_PROMPT", "COMPLETED", null);

    listener.onGenerationCompleted(event);

    ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getNotificationType()).isEqualTo("REVERSE_PROMPT_COMPLETED");
    assertThat(captor.getValue().getTitle()).isEqualTo("역프롬프트 생성이 완료되었습니다.");

    GenerationCompletedPayload payload = GenerationCompletedPayload.from(event);
    assertThat(payload.message()).isEqualTo("역프롬프트 생성이 완료되었습니다.");
  }
}
