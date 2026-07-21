package com.likelion.a1.sse.application.listener;

import com.likelion.a1.generation.domain.event.GenerationCompletedEvent;
import com.likelion.a1.sse.infrastructure.SseRepository;
import com.likelion.a1.sse.presentation.dto.SseDtos.GenerationCompletedPayload;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** GenerationJob이 COMPLETED/FAILED로 커밋된 뒤에만 해당 유저의 SSE Emitter로 결과를 push한다. */
@Component
public class GenerationCompletedEventListener {
  private final SseRepository sseRepository;

  public GenerationCompletedEventListener(SseRepository sseRepository) {
    this.sseRepository = sseRepository;
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onGenerationCompleted(GenerationCompletedEvent event) {
    sseRepository.send(event.userId(), "job-completed", GenerationCompletedPayload.from(event));
  }
}
