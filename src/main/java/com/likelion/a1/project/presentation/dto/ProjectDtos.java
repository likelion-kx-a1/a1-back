package com.likelion.a1.project.presentation.dto;

import java.time.OffsetDateTime;

public final class ProjectDtos {
  private ProjectDtos() {}

  public record CreateRequest(String name, String description) {}

  public record Response(
      Long id,
      Long userId,
      String name,
      String description,
      String status,
      OffsetDateTime createdAt) {}
}
