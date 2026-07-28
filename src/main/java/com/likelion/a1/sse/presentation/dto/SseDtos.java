package com.likelion.a1.sse.presentation.dto;

import com.likelion.a1.generation.domain.event.GenerationCompletedEvent;
import com.likelion.a1.generation.domain.model.GenerationStatus;

public final class SseDtos {
  private SseDtos() {}

  public record ConnectPayload(String eventType, String message) {
    public static ConnectPayload connected() {
      return new ConnectPayload("CONNECTED", "SSE 연결이 성립했습니다.");
    }
  }

  public record GenerationCompletedPayload(
      String eventType,
      Long chatId,
      Long jobId,
      String jobType,
      String status,
      String resultUrl,
      String message) {

    public static GenerationCompletedPayload from(GenerationCompletedEvent event) {
      boolean succeeded = GenerationStatus.COMPLETED.name().equals(event.status());
      boolean video = event.jobType().toUpperCase().contains("VIDEO");
      String mediaLabel = video ? "영상" : "이미지";
      String message =
          succeeded ? mediaLabel + " 생성이 완료되었습니다." : mediaLabel + " 생성에 실패했습니다.";
      return new GenerationCompletedPayload(
          "JOB_COMPLETED",
          event.chatId(),
          event.jobId(),
          event.jobType(),
          event.status(),
          event.resultUrl(),
          message);
    }
  }
}
