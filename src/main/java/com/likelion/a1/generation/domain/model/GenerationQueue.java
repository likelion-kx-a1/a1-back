package com.likelion.a1.generation.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "generation_queue")
public class GenerationQueue {
  @Id
  @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long generationJobId;

  @Column(nullable = false)
  private int priority;

  @Column(nullable = false, length = 30)
  private String status = "WAITING";

  @Column(nullable = false)
  private int retryCount;

  @Column(nullable = false)
  private int maxRetryCount = 3;

  private OffsetDateTime availableAt;
  private OffsetDateTime lockedAt;

  @Column(length = 100)
  private String lockedBy;

  @Column(nullable = false)
  private OffsetDateTime createdAt;

  @Column(nullable = false)
  private OffsetDateTime updatedAt;
}
