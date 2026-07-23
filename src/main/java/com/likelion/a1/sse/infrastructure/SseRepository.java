package com.likelion.a1.sse.infrastructure;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** 사용자 ID별 SseEmitter를 Thread-safe하게 관리한다. 연결 종료/타임아웃/에러 시 즉시 제거해 메모리 누수를 막는다. */
@Component
public class SseRepository {
  private static final long TIMEOUT_MS = 30 * 60 * 1000L;

  private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

  public SseEmitter register(Long userId) {
    SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
    emitters.put(userId, emitter);
    emitter.onCompletion(() -> emitters.remove(userId));
    emitter.onTimeout(() -> emitters.remove(userId));
    emitter.onError(throwable -> emitters.remove(userId));
    return emitter;
  }

  public void send(Long userId, String eventName, Object data) {
    SseEmitter emitter = emitters.get(userId);
    if (emitter == null) {
      return;
    }
    try {
      emitter.send(SseEmitter.event().name(eventName).data(data));
    } catch (IOException exception) {
      emitters.remove(userId);
      emitter.completeWithError(exception);
    }
  }

  public void broadcastHeartbeat() {
    emitters.forEach(
        (userId, emitter) -> {
          try {
            emitter.send(SseEmitter.event().comment("ping"));
          } catch (IOException exception) {
            emitters.remove(userId);
            emitter.completeWithError(exception);
          }
        });
  }
}
