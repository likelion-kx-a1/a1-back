package com.likelion.a1.project.application.service;

import com.likelion.a1.chat.domain.model.Chat;
import com.likelion.a1.chat.domain.repository.ChatRepository;
import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import com.likelion.a1.library.application.service.MyLibraryService;
import com.likelion.a1.media.presentation.dto.MediaDtos.LibraryProjectContentsResponse;
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
  private final ChatRepository chatRepository;
  private final MyLibraryService myLibraryService;

  public ProjectService(
      ProjectRepository projectRepository,
      ChatRepository chatRepository,
      MyLibraryService myLibraryService) {
    this.projectRepository = projectRepository;
    this.chatRepository = chatRepository;
    this.myLibraryService = myLibraryService;
  }

  public Response create(Long userId, CreateRequest request) {
    Project project = Project.create(userId, request.name().trim(), request.description());
    Project savedProject = projectRepository.save(project);

    Chat defaultChat =
        Chat.create(userId, savedProject.getId(), savedProject.getName(), "IMAGE", null);

    Chat savedChat = chatRepository.save(defaultChat);
    Long libraryProjectId =
        myLibraryService
            .createLinkedLibraryProject(userId, savedProject.getId(), savedProject.getName())
            .id();

    return toResponse(savedProject, savedChat.getId(), libraryProjectId);
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
    deleteProjectAndChats(userId, projectId);
    myLibraryService.detachLinkedLibraryProject(userId, projectId);
  }

  public void deleteWithLibrary(Long userId, Long projectId) {
    deleteProjectAndChats(userId, projectId);
    myLibraryService.deleteLinkedLibraryProject(userId, projectId);
  }

  private void deleteProjectAndChats(Long userId, Long projectId) {
    Project project = findOwnedProject(userId, projectId);
    chatRepository.findActiveByUserIdAndProjectId(userId, projectId).stream()
        .forEach(
            chat -> {
              chat.delete();
              chatRepository.save(chat);
            });

    project.delete();
    projectRepository.save(project);
  }

  @Transactional(readOnly = true)
  public LibraryProjectContentsResponse getProjectLibrary(
      Long userId, Long projectId, Long folderId, String assetType, String keyword) {
    findOwnedProject(userId, projectId);
    return myLibraryService.getProjectLibraryContents(userId, projectId, folderId, assetType, keyword);
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
    Long defaultChatId =
        chatRepository
            .findFirstActiveByUserIdAndProjectId(project.getUserId(), project.getId())
            .map(Chat::getId)
            .orElse(null);

    Long libraryProjectId = myLibraryService.findLinkedLibraryProjectId(project.getUserId(), project.getId());

    return toResponse(project, defaultChatId, libraryProjectId);
  }

  private Response toResponse(Project project, Long defaultChatId, Long libraryProjectId) {
    return new Response(
        project.getId(),
        project.getName(),
        project.getDescription(),
        project.getStatus(),
        defaultChatId,
        libraryProjectId,
        project.getCreatedAt(),
        project.getUpdatedAt());
  }
}
