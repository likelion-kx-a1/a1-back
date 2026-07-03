package com.likelion.a1.media.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "generated_media")
public class GeneratedMedia {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long libraryId;

  private Long generationJobId;

  private Long responseMessageId;

  private Long parentMediaId;

  private String mediaType;

  private String title;

  @Column(columnDefinition = "text")
  private String prompt;

  @Column(nullable = false)
  private boolean isSaved;

  private OffsetDateTime savedAt;

  @Column(nullable = false)
  private String status;

  @Column(nullable = false)
  private OffsetDateTime createdAt;

  @Column(nullable = false)
  private OffsetDateTime updatedAt;

  private OffsetDateTime deletedAt;
}
