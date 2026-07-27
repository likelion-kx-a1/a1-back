package com.likelion.a1.project.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public final class ProjectDtos {
  private ProjectDtos() {}

  /**
   * generationType은 선택 항목이다 — null/생략 시 기존과 동일하게 기본 채팅을 IMAGE로 생성한다
   * (하위 호환). VIDEO를 지정하면 기본 채팅이 VIDEO로 생성되어, 클라이언트가 비디오 생성으로
   * 시작할 때 IMAGE 기본 채팅을 우회할 필요 없이 바로 원하는 타입을 받을 수 있다
   * (docs/backend-tasks.md #2).
   */
  public record CreateRequest(
      @NotBlank(message = "프로젝트 이름은 필수입니다.")
      @Size(max = 150, message = "프로젝트 이름은 150자 이하여야 합니다.")
      String name,

      String description,

      @Pattern(regexp = "IMAGE|VIDEO", message = "generationType은 IMAGE 또는 VIDEO만 가능합니다.")
      String generationType) {}

  public record UpdateRequest(
      @NotBlank(message = "프로젝트 이름은 필수입니다.")
      @Size(max = 150, message = "프로젝트 이름은 150자 이하여야 합니다.")
      String name,

      String description) {}

  public record Response(
      Long projectId,
      String name,
      String description,
      String status,
      Long defaultChatId,
      Long libraryProjectId,
      String coverImageUrl,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt) {}
}
