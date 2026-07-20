package com.likelion.a1.project.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "projects")
public class Project {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long userId;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(columnDefinition = "text")
  private String description;

  @Column(nullable = false)
  private String status;

  @Column(nullable = false)
  private OffsetDateTime createdAt;

  @Column(nullable = false)
  private OffsetDateTime updatedAt;

  private OffsetDateTime deletedAt;

  public static Project create(Long userId, String name, String description) {
    Project project = new Project();
    OffsetDateTime now = OffsetDateTime.now();

    project.userId = userId;
    project.name = name;
    project.description = description;
    project.status = "ACTIVE";
    project.createdAt = now;
    project.updatedAt = now;

    return project;
  }

  public void update(String name, String description) {
    this.name = name;
    this.description = description;
    this.updatedAt = OffsetDateTime.now();
  }

  public void delete() {
    OffsetDateTime now = OffsetDateTime.now();

    // Hard delete는 하지 않는다. 목록/상세에서 제외하기 위해 비활성 상태로만 전환한다.
    // 이전 정책은 "DELETED"였지만, 프로젝트 삭제는 status=INACTIVE 소프트 삭제로 통일한다.
    this.status = "INACTIVE";
    this.deletedAt = now;
    this.updatedAt = now;
  }

  public boolean isOwnedBy(Long userId) {
    return this.userId.equals(userId);
  }

  public boolean isDeleted() {
    return this.deletedAt != null || "INACTIVE".equals(this.status) || "DELETED".equals(this.status);
  }
}
