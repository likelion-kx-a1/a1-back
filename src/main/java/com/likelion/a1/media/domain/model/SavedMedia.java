package com.likelion.a1.media.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "saved_media")
public class SavedMedia {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long userId;

  @Column(nullable = false)
  private Long generatedMediaId;

  private Long projectId;
  private Long folderId;

  @Column(nullable = false)
  private String displayName;

  @Column(nullable = false)
  private OffsetDateTime createdAt;

  private OffsetDateTime deletedAt;
}
