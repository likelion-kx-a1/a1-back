package com.likelion.a1.project.presentation.controller;

import com.likelion.a1.global.response.ApiResponse;
import com.likelion.a1.project.application.service.ProjectService;
import com.likelion.a1.project.presentation.dto.ProjectDtos.CreateRequest;
import com.likelion.a1.project.presentation.dto.ProjectDtos.Response;
import com.likelion.a1.project.presentation.dto.ProjectDtos.UpdateRequest;
import com.likelion.a1.user.infrastructure.security.JwtPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
  private final ProjectService projectService;

  public ProjectController(ProjectService projectService) {
    this.projectService = projectService;
  }

  @PostMapping
  public ApiResponse<Response> create(
      @AuthenticationPrincipal JwtPrincipal principal, @Valid @RequestBody CreateRequest request) {
    return ApiResponse.success(
        "PROJECT_CREATED",
        "프로젝트가 생성되었습니다.",
        projectService.create(principal.userId(), request));
  }

  @GetMapping
  public ApiResponse<List<Response>> getProjects(@AuthenticationPrincipal JwtPrincipal principal) {
    return ApiResponse.success(
        "PROJECTS_FETCHED", "프로젝트 목록을 조회했습니다.", projectService.getProjects(principal.userId()));
  }

  @GetMapping("/{projectId}")
  public ApiResponse<Response> getProject(
      @AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long projectId) {
    return ApiResponse.success(
        "PROJECT_FETCHED",
        "프로젝트를 조회했습니다.",
        projectService.getProject(principal.userId(), projectId));
  }

  @PatchMapping("/{projectId}")
  public ApiResponse<Response> update(
      @AuthenticationPrincipal JwtPrincipal principal,
      @PathVariable Long projectId,
      @Valid @RequestBody UpdateRequest request) {
    return ApiResponse.success(
        "PROJECT_UPDATED",
        "프로젝트가 수정되었습니다.",
        projectService.update(principal.userId(), projectId, request));
  }

  @DeleteMapping("/{projectId}")
  public ApiResponse<Void> delete(
      @AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long projectId) {
    projectService.delete(principal.userId(), projectId);

    return ApiResponse.success("PROJECT_DELETED", "프로젝트가 삭제되었습니다.", null);
  }
}
