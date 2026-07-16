package com.likelion.a1.media.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "generated_assets")
public class GeneratedAsset {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long userId;

  @Column(nullable = false)
  private Long chatId;

  private Long generationJobId;
  private Long responseMessageId;
  private Long parentAssetId;

  @Column(nullable = false, length = 20)
  private String assetType;

  @Column(length = 30)
  private String imageCategory;

  private String title;

  @Column(columnDefinition = "text")
  private String prompt;

  @Column(nullable = false, length = 30)
  private String status = "ACTIVE";

  @Column(nullable = false)
  private OffsetDateTime createdAt;

  @Column(nullable = false)
  private OffsetDateTime updatedAt;

  private OffsetDateTime deletedAt;

  public static GeneratedAsset create(
      Long userId,
      Long chatId,
      Long generationJobId,
      Long responseMessageId,
      Long parentAssetId,
      String assetType,
      String imageCategory,
      String title,
      String prompt) {
    GeneratedAsset asset = new GeneratedAsset();
    OffsetDateTime now = OffsetDateTime.now();

    asset.userId = userId;
    asset.chatId = chatId;
    asset.generationJobId = generationJobId;
    asset.responseMessageId = responseMessageId;
    asset.parentAssetId = parentAssetId;
    asset.assetType = assetType;
    asset.imageCategory = imageCategory;
    asset.title = title;
    asset.prompt = prompt;
    asset.status = "ACTIVE";
    asset.createdAt = now;
    asset.updatedAt = now;

    return asset;
  }

  public boolean isOwnedBy(Long userId) {
    return this.userId.equals(userId);
  }

  public boolean isDeleted() {
    return this.deletedAt != null || "DELETED".equals(this.status);
  }
}
