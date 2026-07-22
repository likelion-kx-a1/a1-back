package com.likelion.a1.sse.application.service;

import com.likelion.a1.sse.infrastructure.SseRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Nginx/ALB의 유휴 커넥션 종료를 막기 위해 30초마다 모든 SSE Emitter에 ping 코멘트를 보낸다. */
@Component
public class SseHeartbeatScheduler {
  private final SseRepository sseRepository;

  public SseHeartbeatScheduler(SseRepository sseRepository) {
    this.sseRepository = sseRepository;
  }

  @Scheduled(fixedRate = 30000)
  public void sendHeartbeat() {
    sseRepository.broadcastHeartbeat();
  }
}
