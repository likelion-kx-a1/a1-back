package com.likelion.a1.project.application.service;

import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import com.likelion.a1.project.domain.model.Project;
import com.likelion.a1.project.domain.repository.ProjectRepository;
import com.likelion.a1.project.presentation.dto.ProjectDtos.CreateRequest;
import com.likelion.a1.project.presentation.dto.ProjectDtos.Response;
import com.likelion.a1.project.presentation.dto.ProjectDtos.UpdateRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProjectService {
  private final ProjectRepository projectRepository;

  public ProjectService(ProjectRepository projectRepository) {
    this.projectRepository = projectRepository;
  }

  public Response create(Long userId, CreateRequest request) {
    Project project = Project.create(userId, request.name().trim(), request.description());

    return toResponse(projectRepository.save(project));
  }

  @Transactional(readOnly = true)
  public List<Response> getProjects(Long userId) {
    return projectRepository.findActiveByUserId(userId).stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public Response getProject(Long userId, Long projectId) {
    return toResponse(findOwnedProject(userId, projectId));
  }

  public Response update(Long userId, Long projectId, UpdateRequest request) {
    Project project = findOwnedProject(userId, projectId);
    project.update(request.name().trim(), request.description());

    return toResponse(projectRepository.save(project));
  }

  public void delete(Long userId, Long projectId) {
    Project project = findOwnedProject(userId, projectId);
    project.delete();

    projectRepository.save(project);
  }

  public Project findOwnedProject(Long userId, Long projectId) {
    Project project =
        projectRepository
            .findById(projectId)
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));

    if (project.isDeleted() || !project.isOwnedBy(userId)) {
      throw new BusinessException(ErrorCode.INVALID_INPUT);
    }

    return project;
  }

  private Response toResponse(Project project) {
    return new Response(
        project.getId(),
        project.getName(),
        project.getDescription(),
        project.getStatus(),
        project.getCreatedAt(),
        project.getUpdatedAt());
  }
}
