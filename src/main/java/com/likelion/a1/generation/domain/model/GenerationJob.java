package com.likelion.a1.generation.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "generation_jobs")
public class GenerationJob {
  @Id
  @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long userId;

  @Column(nullable = false)
  private Long chatId;

  private Long aiModelId;
  private Long requestMessageId;

  @Column(nullable = false, length = 20)
  private String generationType;

  @Column(length = 30)
  private String imageCategory;

  @Column(columnDefinition = "text")
  private String prompt;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> requestPayload;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> responsePayload;

  @Column(nullable = false, length = 30)
  private String status = "PENDING";

  @Column(columnDefinition = "text")
  private String errorMessage;

  private OffsetDateTime startedAt;
  private OffsetDateTime completedAt;

  @Column(nullable = false)
  private OffsetDateTime createdAt;

  @Column(nullable = false)
  private OffsetDateTime updatedAt;

  public static GenerationJob create(
      Long userId,
      Long chatId,
      Long aiModelId,
      Long requestMessageId,
      String jobType,
      String prompt,
      Map<String, Object> requestPayload) {
    GenerationJob job = new GenerationJob();
    job.userId = userId;
    job.chatId = chatId;
    job.aiModelId = aiModelId;
    job.requestMessageId = requestMessageId;
    job.generationType = jobType;
    job.prompt = prompt;
    job.requestPayload = requestPayload;
    job.status = GenerationStatus.PROCESSING.name();
    job.startedAt = OffsetDateTime.now();
    job.createdAt = job.startedAt;
    job.updatedAt = job.startedAt;
    return job;
  }

  public void markQueued(Map<String, Object> responsePayload) {
    applyStatus(GenerationStatus.QUEUED, responsePayload, null);
  }

  public void complete(Map<String, Object> responsePayload) {
    applyStatus(GenerationStatus.COMPLETED, responsePayload, null);
  }

  public void fail(String errorMessage) {
    applyStatus(GenerationStatus.FAILED, this.responsePayload, errorMessage);
  }

  public void applyPolledStatus(GenerationStatus status, Map<String, Object> responsePayload) {
    applyStatus(status, responsePayload, null);
  }

  private void applyStatus(GenerationStatus status, Map<String, Object> responsePayload, String errorMessage) {
    this.status = status.name();
    this.responsePayload = responsePayload;
    this.errorMessage = errorMessage;
    this.updatedAt = OffsetDateTime.now();
    if (status == GenerationStatus.COMPLETED || status == GenerationStatus.FAILED) {
      this.completedAt = this.updatedAt;
    }
  }
}
