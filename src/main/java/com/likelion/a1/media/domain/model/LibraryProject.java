package com.likelion.a1.media.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "library_projects")
public class LibraryProject {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long userId;

  private Long parentProjectId;

  private Long sourceProjectId;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(nullable = false)
  private int depth;

  @Column(nullable = false, length = 30)
  private String status = "ACTIVE";

  @Column(nullable = false)
  private OffsetDateTime createdAt;

  @Column(nullable = false)
  private OffsetDateTime updatedAt;

  private OffsetDateTime deletedAt;

  public static LibraryProject create(Long userId, Long parentProjectId, String name, int depth) {
    return create(userId, parentProjectId, null, name, depth);
  }

  public static LibraryProject create(
      Long userId, Long parentProjectId, Long sourceProjectId, String name, int depth) {
    LibraryProject project = new LibraryProject();
    OffsetDateTime now = OffsetDateTime.now();

    project.userId = userId;
    project.parentProjectId = parentProjectId;
    project.sourceProjectId = sourceProjectId;
    project.name = name;
    project.depth = depth;
    project.status = "ACTIVE";
    project.createdAt = now;
    project.updatedAt = now;

    return project;
  }

  public void updateName(String name) {
    this.name = name;
    this.updatedAt = OffsetDateTime.now();
  }

  public void delete() {
    OffsetDateTime now = OffsetDateTime.now();

    this.status = "DELETED";
    this.deletedAt = now;
    this.updatedAt = now;
  }

  public void detachSourceProject() {
    this.sourceProjectId = null;
    this.updatedAt = OffsetDateTime.now();
  }

  public boolean isOwnedBy(Long userId) {
    return this.userId.equals(userId);
  }

  public boolean isDeleted() {
    return this.deletedAt != null || "DELETED".equals(this.status);
  }
}
