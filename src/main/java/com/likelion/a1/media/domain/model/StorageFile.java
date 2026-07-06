package com.likelion.a1.media.domain.model;
//실제 파일 저장위치

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "storage_files")
public class StorageFile {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long generatedMediaId;

  private String fileType;

  @Column(nullable = false)
  private String bucketName;

  @Column(nullable = false, columnDefinition = "text")
  private String storagePath;

  @Column(columnDefinition = "text")
  private String publicUrl;

  private String originalFilename;

  private String mimeType;

  private Long fileSize;
  private Integer width;

  private Integer height;

  private Integer durationSeconds;

  @Column(nullable = false)
  private OffsetDateTime createdAt;
}
