package com.likelion.a1.media.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "downloads")
public class Download {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long userId;

  @Column(nullable = false)
  private Long generatedAssetId;

  private Long assetFileId;

  @Column(nullable = false)
  private String downloadFilename;

  @Column(length = 50)
  private String ipAddress;

  @Column(columnDefinition = "text")
  private String userAgent;

  @Column(nullable = false)
  private OffsetDateTime downloadedAt;
}
