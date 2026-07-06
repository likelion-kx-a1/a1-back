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
  private Long libraryId;

  private Long requestMessageId;
  private String modelName;

  private String jobType;

  @Column(columnDefinition = "text")
  private String prompt;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> requestPayload;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> responsePayload;

  private String status;

  @Column(columnDefinition = "text")
  private String errorMessage;

  private OffsetDateTime startedAt;

  private OffsetDateTime completedAt;

  @Column(nullable = false)
  private OffsetDateTime createdAt;

  @Column(nullable = false)
  private OffsetDateTime updatedAt;
}
