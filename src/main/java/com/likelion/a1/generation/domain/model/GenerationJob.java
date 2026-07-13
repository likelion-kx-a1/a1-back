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
}
