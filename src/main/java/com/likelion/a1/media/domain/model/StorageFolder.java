package com.likelion.a1.media.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "storage_folders")
public class StorageFolder {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long userId;

  private Long parentFolderId;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(nullable = false, length = 30)
  private String status = "ACTIVE";

  @Column(nullable = false)
  private OffsetDateTime createdAt;

  @Column(nullable = false)
  private OffsetDateTime updatedAt;

  private OffsetDateTime deletedAt;
}
