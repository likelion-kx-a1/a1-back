package com.likelion.a1.media.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "asset_files")
public class AssetFile {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long generatedAssetId;

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

  public static AssetFile create(
      Long generatedAssetId,
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
    AssetFile file = new AssetFile();

    file.generatedAssetId = generatedAssetId;
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
