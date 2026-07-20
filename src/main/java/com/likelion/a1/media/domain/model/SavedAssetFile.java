package com.likelion.a1.media.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "saved_asset_files")
public class SavedAssetFile {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long savedAssetId;

  private Long sourceAssetFileId;

  @Column(length = 30)
  private String fileType;

  @Column(nullable = false, length = 100)
  private String bucketName;

  @Column(nullable = false, columnDefinition = "text")
  private String storagePath;

  @Column(columnDefinition = "text")
  private String publicUrl;

  private String originalFilename;
  private String storedFilename;

  @Column(length = 100)
  private String mimeType;

  private Long fileSize;
  private Integer width;
  private Integer height;
  private Integer durationSeconds;

  @Column(nullable = false)
  private OffsetDateTime createdAt;

  public static SavedAssetFile create(
      Long savedAssetId,
      Long sourceAssetFileId,
      String fileType,
      String bucketName,
      String storagePath,
      String publicUrl,
      String originalFilename,
      String storedFilename,
      String mimeType,
      Long fileSize,
      Integer width,
      Integer height,
      Integer durationSeconds) {
    SavedAssetFile file = new SavedAssetFile();

    file.savedAssetId = savedAssetId;
    file.sourceAssetFileId = sourceAssetFileId;
    file.fileType = fileType;
    file.bucketName = bucketName;
    file.storagePath = storagePath;
    file.publicUrl = publicUrl;
    file.originalFilename = originalFilename;
    file.storedFilename = storedFilename;
    file.mimeType = mimeType;
    file.fileSize = fileSize;
    file.width = width;
    file.height = height;
    file.durationSeconds = durationSeconds;
    file.createdAt = OffsetDateTime.now();

    return file;
  }
}
