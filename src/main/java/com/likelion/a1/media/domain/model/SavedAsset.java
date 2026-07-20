package com.likelion.a1.media.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "saved_assets")
public class SavedAsset {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long userId;

  private Long libraryProjectId;

  private Long folderId;

  private Long sourceGeneratedAssetId;

  @Column(nullable = false, length = 30)
  private String sourceType = "GENERATED_ASSET";

  private Long sourceChatId;
  private Long sourceMessageId;
  private Long sourceMessageFileId;

  @Column(nullable = false, length = 20)
  private String assetType;

  @Column(nullable = false)
  private String displayName;

  @Column(nullable = false, length = 30)
  private String status = "ACTIVE";

  @Column(nullable = false)
  private OffsetDateTime createdAt;

  @Column(nullable = false)
  private OffsetDateTime updatedAt;

  private OffsetDateTime deletedAt;

  public static SavedAsset create(
      Long userId,
      Long libraryProjectId,
      Long folderId,
      Long sourceGeneratedAssetId,
      String sourceType,
      Long sourceChatId,
      Long sourceMessageId,
      Long sourceMessageFileId,
      String assetType,
      String displayName) {
    SavedAsset savedAsset = new SavedAsset();
    OffsetDateTime now = OffsetDateTime.now();

    savedAsset.userId = userId;
    savedAsset.libraryProjectId = libraryProjectId;
    savedAsset.folderId = folderId;
    savedAsset.sourceGeneratedAssetId = sourceGeneratedAssetId;
    savedAsset.sourceType = sourceType;
    savedAsset.sourceChatId = sourceChatId;
    savedAsset.sourceMessageId = sourceMessageId;
    savedAsset.sourceMessageFileId = sourceMessageFileId;
    savedAsset.assetType = assetType;
    savedAsset.displayName = displayName;
    savedAsset.status = "ACTIVE";
    savedAsset.createdAt = now;
    savedAsset.updatedAt = now;

    return savedAsset;
  }

  public void moveToFolder(Long folderId) {
    this.folderId = folderId;
    this.updatedAt = OffsetDateTime.now();
  }

  public void updateDisplayName(String displayName) {
    this.displayName = displayName;
    this.updatedAt = OffsetDateTime.now();
  }

  public void delete() {
    OffsetDateTime now = OffsetDateTime.now();

    this.status = "DELETED";
    this.deletedAt = now;
    this.updatedAt = now;
  }

  public boolean isOwnedBy(Long userId) {
    return this.userId.equals(userId);
  }

  public boolean isDeleted() {
    return this.deletedAt != null || "DELETED".equals(this.status);
  }
}
