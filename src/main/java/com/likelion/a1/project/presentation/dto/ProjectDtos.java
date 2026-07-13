package com.likelion.a1.project.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public final class ProjectDtos {
  private ProjectDtos() {}

  public record CreateRequest(
      @NotBlank(message = "프로젝트 이름은 필수입니다.")
      @Size(max = 150, message = "프로젝트 이름은 150자 이하여야 합니다.")
      String name,

      String description) {}

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
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt) {}
}
