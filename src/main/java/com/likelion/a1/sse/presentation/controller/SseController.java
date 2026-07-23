package com.likelion.a1.sse.presentation.controller;

import com.likelion.a1.sse.infrastructure.SseRepository;
import com.likelion.a1.sse.presentation.dto.SseDtos.ConnectPayload;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** 3초 폴링을 대체하는 전역 SSE 알림 구독 엔드포인트. */
@RestController
@RequestMapping("/api/v1/sse")
public class SseController {
  private final SseRepository sseRepository;

  public SseController(SseRepository sseRepository) {
    this.sseRepository = sseRepository;
  }

  @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public ResponseEntity<SseEmitter> subscribe(@RequestParam Long userId) {
    SseEmitter emitter = sseRepository.register(userId);
    sseRepository.send(userId, "connect", ConnectPayload.connected());

    return ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL, "no-cache")
        .header("X-Accel-Buffering", "no")
        .body(emitter);
  }
}
