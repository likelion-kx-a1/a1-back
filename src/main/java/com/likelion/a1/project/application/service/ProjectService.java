package com.likelion.a1.project.application.service;

import com.likelion.a1.chat.domain.model.Chat;
import com.likelion.a1.chat.domain.model.ChatMessage;
import com.likelion.a1.chat.domain.model.ChatMessageFile;
import com.likelion.a1.chat.domain.repository.ChatMessageFileRepository;
import com.likelion.a1.chat.domain.repository.ChatMessageRepository;
import com.likelion.a1.chat.domain.repository.ChatRepository;
import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import com.likelion.a1.library.application.service.MyLibraryService;
import com.likelion.a1.media.presentation.dto.MediaDtos.LibraryProjectContentsResponse;
import com.likelion.a1.project.domain.model.Project;
import com.likelion.a1.project.domain.repository.ProjectRepository;
import com.likelion.a1.project.presentation.dto.ProjectDtos.CreateRequest;
import com.likelion.a1.project.presentation.dto.ProjectDtos.Response;
import com.likelion.a1.project.presentation.dto.ProjectDtos.ThumbnailResponse;
import com.likelion.a1.project.presentation.dto.ProjectDtos.UpdateRequest;
import java.util.List;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProjectService {
  private final ProjectRepository projectRepository;
  private final ChatRepository chatRepository;
  private final MyLibraryService myLibraryService;
  private final ChatMessageRepository chatMessageRepository;
  private final ChatMessageFileRepository chatMessageFileRepository;

  public ProjectService(
      ProjectRepository projectRepository,
      ChatRepository chatRepository,
      MyLibraryService myLibraryService,
      ChatMessageRepository chatMessageRepository,
      ChatMessageFileRepository chatMessageFileRepository) {
    this.projectRepository = projectRepository;
    this.chatRepository = chatRepository;
    this.myLibraryService = myLibraryService;
    this.chatMessageRepository = chatMessageRepository;
    this.chatMessageFileRepository = chatMessageFileRepository;
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
    findOwnedProject(userId, projectId);
    deleteProjectChats(userId, projectId);
  }

  public void deleteWithLibrary(Long userId, Long projectId) {
    Project project = findOwnedProject(userId, projectId);
    deleteProjectChats(userId, projectId);
    myLibraryService.deleteLinkedLibraryProject(userId, projectId);
    project.delete();
    projectRepository.save(project);
  }

  private void deleteProjectChats(Long userId, Long projectId) {
    chatRepository.findActiveByUserIdAndProjectId(userId, projectId).stream()
        .forEach(
            chat -> {
              chat.delete();
              chatRepository.save(chat);
            });
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
        resolveThumbnail(project),
        project.getCreatedAt(),
        project.getUpdatedAt());
  }

  private ThumbnailResponse resolveThumbnail(Project project) {
    List<ThumbnailCandidate> candidates =
        chatRepository.findActiveByUserIdAndProjectId(project.getUserId(), project.getId()).stream()
            .flatMap(
                chat -> {
                  List<ChatMessage> messages = chatMessageRepository.findActiveByChatId(chat.getId());
                  if (messages.isEmpty()) {
                    return java.util.stream.Stream.empty();
                  }
                  Map<Long, ChatMessage> messageById =
                      messages.stream().collect(Collectors.toMap(ChatMessage::getId, message -> message));
                  return chatMessageFileRepository
                      .findByMessageIds(messageById.keySet())
                      .stream()
                      .map(file -> new ThumbnailCandidate(chat.getId(), messageById.get(file.getMessageId()), file));
                })
            .filter(candidate -> candidate.message() != null)
            .toList();

    Predicate<ThumbnailCandidate> generated =
        candidate ->
            candidate.message().getGeneratedAssetId() != null
                && isPreviewable(candidate.file());
    Predicate<ThumbnailCandidate> uploadedImage =
        candidate ->
            "USER".equalsIgnoreCase(candidate.message().getSenderType())
                && "IMAGE".equalsIgnoreCase(candidate.file().getFileType());

    return candidates.stream()
        .filter(generated)
        .min(Comparator.comparing(candidate -> candidate.file().getCreatedAt()))
        .or(() ->
            candidates.stream()
                .filter(uploadedImage)
                .min(Comparator.comparing(candidate -> candidate.file().getCreatedAt())))
        .map(this::toThumbnailResponse)
        .orElse(null);
  }

  private boolean isPreviewable(ChatMessageFile file) {
    return "IMAGE".equalsIgnoreCase(file.getFileType())
        || "VIDEO".equalsIgnoreCase(file.getFileType());
  }

  private ThumbnailResponse toThumbnailResponse(ThumbnailCandidate candidate) {
    ChatMessageFile file = candidate.file();
    return new ThumbnailResponse(
        candidate.chatId(),
        file.getId(),
        file.getFileType(),
        file.getPublicUrl(),
        file.getOriginalFilename());
  }

  private record ThumbnailCandidate(Long chatId, ChatMessage message, ChatMessageFile file) {}
}
