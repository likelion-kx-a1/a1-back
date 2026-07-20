package com.likelion.a1.library.application.service;

import com.likelion.a1.chat.domain.model.Chat;
import com.likelion.a1.chat.domain.model.ChatMessage;
import com.likelion.a1.chat.domain.model.ChatMessageFile;
import com.likelion.a1.chat.domain.repository.ChatMessageFileRepository;
import com.likelion.a1.chat.domain.repository.ChatMessageRepository;
import com.likelion.a1.chat.domain.repository.ChatRepository;
import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import com.likelion.a1.media.application.port.out.MediaStoragePort;
import com.likelion.a1.media.application.port.out.StorageDownloadResult;
import com.likelion.a1.media.application.port.out.StorageUploadCommand;
import com.likelion.a1.media.application.port.out.StorageUploadResult;
import com.likelion.a1.media.domain.model.AssetFile;
import com.likelion.a1.media.domain.model.GeneratedAsset;
import com.likelion.a1.media.domain.model.LibraryProject;
import com.likelion.a1.media.domain.model.SavedAsset;
import com.likelion.a1.media.domain.model.SavedAssetFile;
import com.likelion.a1.media.domain.repository.AssetFileRepository;
import com.likelion.a1.media.domain.repository.GeneratedAssetRepository;
import com.likelion.a1.media.domain.repository.LibraryProjectRepository;
import com.likelion.a1.media.domain.repository.SavedAssetFileRepository;
import com.likelion.a1.media.domain.repository.SavedAssetRepository;
import com.likelion.a1.media.presentation.dto.MediaDtos.CreateLibraryProjectRequest;
import com.likelion.a1.media.presentation.dto.MediaDtos.LibraryAssetResponse;
import com.likelion.a1.media.presentation.dto.MediaDtos.LibraryProjectContentsResponse;
import com.likelion.a1.media.presentation.dto.MediaDtos.LibraryProjectResponse;
import com.likelion.a1.media.presentation.dto.MediaDtos.LibraryProjectSummaryResponse;
import com.likelion.a1.media.presentation.dto.MediaDtos.LibrarySourceChatResponse;
import com.likelion.a1.media.presentation.dto.MediaDtos.LibrarySourceGeneratedAssetResponse;
import com.likelion.a1.media.presentation.dto.MediaDtos.LibrarySourceMessageResponse;
import com.likelion.a1.media.presentation.dto.MediaDtos.SaveAssetRequest;
import com.likelion.a1.media.presentation.dto.MediaDtos.SavedAssetFileResponse;
import com.likelion.a1.media.presentation.dto.MediaDtos.SavedAssetResponse;
import com.likelion.a1.media.presentation.dto.MediaDtos.UpdateLibraryProjectRequest;
import com.likelion.a1.media.presentation.dto.MediaDtos.UpdateSavedAssetRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class MyLibraryService {
  private static final String ASSET_TYPE_IMAGE = "IMAGE";
  private static final String ASSET_TYPE_VIDEO = "VIDEO";
  private static final String SOURCE_TYPE_GENERATED_ASSET = "GENERATED_ASSET";
  private static final String SOURCE_TYPE_CHAT_MESSAGE_FILE = "CHAT_MESSAGE_FILE";

  private final LibraryProjectRepository libraryProjectRepository;
  private final SavedAssetRepository savedAssetRepository;
  private final SavedAssetFileRepository savedAssetFileRepository;
  private final GeneratedAssetRepository generatedAssetRepository;
  private final AssetFileRepository assetFileRepository;
  private final MediaStoragePort mediaStoragePort;
  private final ChatRepository chatRepository;
  private final ChatMessageRepository chatMessageRepository;
  private final ChatMessageFileRepository chatMessageFileRepository;

  public MyLibraryService(
      LibraryProjectRepository libraryProjectRepository,
      SavedAssetRepository savedAssetRepository,
      SavedAssetFileRepository savedAssetFileRepository,
      GeneratedAssetRepository generatedAssetRepository,
      AssetFileRepository assetFileRepository,
      MediaStoragePort mediaStoragePort,
      ChatRepository chatRepository,
      ChatMessageRepository chatMessageRepository,
      ChatMessageFileRepository chatMessageFileRepository) {
    this.libraryProjectRepository = libraryProjectRepository;
    this.savedAssetRepository = savedAssetRepository;
    this.savedAssetFileRepository = savedAssetFileRepository;
    this.generatedAssetRepository = generatedAssetRepository;
    this.assetFileRepository = assetFileRepository;
    this.mediaStoragePort = mediaStoragePort;
    this.chatRepository = chatRepository;
    this.chatMessageRepository = chatMessageRepository;
    this.chatMessageFileRepository = chatMessageFileRepository;
  }

  public LibraryProjectResponse createLibraryProject(
      Long userId, CreateLibraryProjectRequest request) {
    Long parentProjectId = request.parentProjectId();
    int depth = 0;
    if (parentProjectId != null) {
      LibraryProject parent = findOwnedLibraryProject(userId, parentProjectId);
      if (parent.getDepth() >= 1 || parent.getParentProjectId() != null) {
        throw new BusinessException(ErrorCode.INVALID_STORAGE_FOLDER_DEPTH);
      }
      depth = parent.getDepth() + 1;
    }

    LibraryProject project =
        LibraryProject.create(userId, parentProjectId, normalizeRequired(request.name()), depth);

    return toLibraryProjectResponse(libraryProjectRepository.save(project));
  }

  @Transactional(readOnly = true)
  public List<LibraryProjectResponse> getLibraryProjects(Long userId, Long parentProjectId) {
    List<LibraryProject> projects =
        parentProjectId == null
            ? libraryProjectRepository.findActiveByUserId(userId).stream()
                .filter(project -> project.getParentProjectId() == null)
                .toList()
            : libraryProjectRepository.findActiveByUserIdAndParentProjectId(userId, parentProjectId);

    return projects.stream().map(this::toLibraryProjectResponse).toList();
  }

  @Transactional(readOnly = true)
  public LibraryProjectContentsResponse getLibraryProjectContents(
      Long userId, Long libraryProjectId, String assetType, String keyword) {
    LibraryProject project = findOwnedLibraryProject(userId, libraryProjectId);
    List<LibraryProjectResponse> childProjects =
        libraryProjectRepository.findActiveByUserIdAndParentProjectId(userId, project.getId())
            .stream()
            .map(this::toLibraryProjectResponse)
            .toList();

    List<SavedAsset> savedAssets =
        savedAssetRepository.findActiveByUserId(
            userId,
            project.getId(),
            null,
            normalizeNullableUpper(assetType),
            normalizeNullable(keyword));
    Map<Long, List<SavedAssetFile>> filesBySavedAssetId =
        savedAssetFileRepository.findBySavedAssetIds(
                savedAssets.stream().map(SavedAsset::getId).toList())
            .stream()
            .collect(Collectors.groupingBy(SavedAssetFile::getSavedAssetId));
    LibraryProjectSummaryResponse projectSummary = toLibraryProjectSummaryResponse(project);

    List<LibraryAssetResponse> assets =
        savedAssets.stream()
            .map(
                savedAsset ->
                    toLibraryAssetResponse(
                        savedAsset,
                        projectSummary,
                        filesBySavedAssetId.getOrDefault(savedAsset.getId(), List.of())))
            .toList();

    return new LibraryProjectContentsResponse(
        toLibraryProjectResponse(project), childProjects, assets);
  }

  public LibraryProjectResponse updateLibraryProject(
      Long userId, Long libraryProjectId, UpdateLibraryProjectRequest request) {
    LibraryProject project = findOwnedLibraryProject(userId, libraryProjectId);
    project.updateName(normalizeRequired(request.name()));

    return toLibraryProjectResponse(libraryProjectRepository.save(project));
  }

  public void deleteLibraryProject(Long userId, Long libraryProjectId) {
    LibraryProject project = findOwnedLibraryProject(userId, libraryProjectId);
    deleteProjectContents(userId, project.getId());

    libraryProjectRepository.findActiveByUserIdAndParentProjectId(userId, project.getId()).stream()
        .forEach(
            childProject -> {
              deleteProjectContents(userId, childProject.getId());
              childProject.delete();
              libraryProjectRepository.save(childProject);
            });

    project.delete();
    libraryProjectRepository.save(project);
  }

  public SavedAssetResponse saveAsset(Long userId, SaveAssetRequest request) {
    LibraryProject project = findOwnedLibraryProject(userId, request.libraryProjectId());
    String sourceType = resolveSourceType(request);

    if (SOURCE_TYPE_CHAT_MESSAGE_FILE.equals(sourceType)) {
      return saveChatMessageFileAsset(userId, project, request);
    }

    return saveGeneratedAsset(userId, project, request);
  }

  private SavedAssetResponse saveGeneratedAsset(
      Long userId, LibraryProject project, SaveAssetRequest request) {
    GeneratedAsset generatedAsset = findOwnedGeneratedAsset(userId, request.generatedAssetId());
    String assetType = normalizeAssetType(generatedAsset.getAssetType());

    SavedAsset savedAsset =
        savedAssetRepository.save(
            SavedAsset.create(
                userId,
                project.getId(),
                null,
                generatedAsset.getId(),
                SOURCE_TYPE_GENERATED_ASSET,
                generatedAsset.getChatId(),
                generatedAsset.getResponseMessageId(),
                null,
                assetType,
                resolveDisplayName(request.displayName(), generatedAsset)));

    List<SavedAssetFile> copiedFiles = copyGeneratedAssetFiles(userId, savedAsset);
    savedAssetFileRepository.saveAll(copiedFiles);

    return toSavedAssetResponse(savedAsset, copiedFiles);
  }

  private SavedAssetResponse saveChatMessageFileAsset(
      Long userId, LibraryProject project, SaveAssetRequest request) {
    ChatMessageFile sourceFile = findOwnedChatMessageFile(userId, request.chatMessageFileId());
    ChatMessage sourceMessage = findActiveMessage(sourceFile.getMessageId());
    String assetType = normalizeFileAssetType(sourceFile.getFileType(), sourceFile.getMimeType());

    SavedAsset savedAsset =
        savedAssetRepository.save(
            SavedAsset.create(
                userId,
                project.getId(),
                null,
                null,
                SOURCE_TYPE_CHAT_MESSAGE_FILE,
                sourceMessage.getChatId(),
                sourceMessage.getId(),
                sourceFile.getId(),
                assetType,
                resolveDisplayName(request.displayName(), sourceFile)));

    SavedAssetFile copiedFile = copyChatMessageFile(userId, savedAsset, sourceFile);
    List<SavedAssetFile> copiedFiles = savedAssetFileRepository.saveAll(List.of(copiedFile));

    return toSavedAssetResponse(savedAsset, copiedFiles);
  }

  @Transactional(readOnly = true)
  public List<SavedAssetResponse> getSavedAssets(
      Long userId, Long libraryProjectId, String assetType, String keyword) {
    if (libraryProjectId != null) {
      findOwnedLibraryProject(userId, libraryProjectId);
    }

    List<SavedAsset> savedAssets =
        savedAssetRepository.findActiveByUserId(
            userId,
            libraryProjectId,
            null,
            normalizeNullableUpper(assetType),
            normalizeNullable(keyword));
    Map<Long, List<SavedAssetFile>> filesBySavedAssetId =
        savedAssetFileRepository.findBySavedAssetIds(
                savedAssets.stream().map(SavedAsset::getId).toList())
            .stream()
            .collect(Collectors.groupingBy(SavedAssetFile::getSavedAssetId));

    return savedAssets.stream()
        .map(
            savedAsset ->
                toSavedAssetResponse(
                    savedAsset, filesBySavedAssetId.getOrDefault(savedAsset.getId(), List.of())))
        .toList();
  }

  @Transactional(readOnly = true)
  public SavedAssetResponse getSavedAsset(Long userId, Long savedAssetId) {
    SavedAsset savedAsset = findOwnedSavedAsset(userId, savedAssetId);

    return toSavedAssetResponse(
        savedAsset, savedAssetFileRepository.findBySavedAssetId(savedAsset.getId()));
  }

  public SavedAssetResponse updateSavedAsset(
      Long userId, Long savedAssetId, UpdateSavedAssetRequest request) {
    SavedAsset savedAsset = findOwnedSavedAsset(userId, savedAssetId);
    savedAsset.updateDisplayName(normalizeRequired(request.displayName()));

    return toSavedAssetResponse(
        savedAssetRepository.save(savedAsset),
        savedAssetFileRepository.findBySavedAssetId(savedAsset.getId()));
  }

  public void deleteSavedAsset(Long userId, Long savedAssetId) {
    SavedAsset savedAsset = findOwnedSavedAsset(userId, savedAssetId);
    savedAsset.delete();

    savedAssetRepository.save(savedAsset);
  }

  private void deleteProjectContents(Long userId, Long libraryProjectId) {
    savedAssetRepository.findActiveByUserId(userId, libraryProjectId, null, null, null).stream()
        .forEach(
            savedAsset -> {
              savedAsset.delete();
              savedAssetRepository.save(savedAsset);
            });
  }

  private List<SavedAssetFile> copyGeneratedAssetFiles(Long userId, SavedAsset savedAsset) {
    List<AssetFile> sourceFiles =
        assetFileRepository.findByGeneratedAssetId(savedAsset.getSourceGeneratedAssetId());
    if (sourceFiles.isEmpty()) {
      throw new BusinessException(ErrorCode.ASSET_FILE_NOT_FOUND);
    }

    return sourceFiles.stream()
        .map(sourceFile -> copyGeneratedAssetFile(userId, savedAsset, sourceFile))
        .toList();
  }

  private SavedAssetFile copyGeneratedAssetFile(
      Long userId, SavedAsset savedAsset, AssetFile sourceFile) {
    StorageDownloadResult downloaded =
        mediaStoragePort.download(sourceFile.getBucketName(), sourceFile.getStoragePath());
    StorageUploadResult uploaded =
        mediaStoragePort.upload(
            new StorageUploadCommand(
                downloaded.content(),
                sourceFile.getOriginalFilename(),
                resolveContentType(downloaded.contentType(), sourceFile.getMimeType()),
                null,
                resolveLibraryDirectory(userId, savedAsset)));

    return SavedAssetFile.create(
        savedAsset.getId(),
        sourceFile.getId(),
        sourceFile.getFileType(),
        uploaded.bucketName(),
        uploaded.storagePath(),
        uploaded.publicUrl(),
        sourceFile.getOriginalFilename(),
        uploaded.storedFilename(),
        uploaded.mimeType(),
        uploaded.fileSize(),
        sourceFile.getWidth(),
        sourceFile.getHeight(),
        sourceFile.getDurationSeconds());
  }

  private SavedAssetFile copyChatMessageFile(
      Long userId, SavedAsset savedAsset, ChatMessageFile sourceFile) {
    StorageDownloadResult downloaded =
        mediaStoragePort.download(sourceFile.getBucketName(), sourceFile.getStoragePath());
    StorageUploadResult uploaded =
        mediaStoragePort.upload(
            new StorageUploadCommand(
                downloaded.content(),
                sourceFile.getOriginalFilename(),
                resolveContentType(downloaded.contentType(), sourceFile.getMimeType()),
                null,
                resolveLibraryDirectory(userId, savedAsset)));

    return SavedAssetFile.create(
        savedAsset.getId(),
        null,
        sourceFile.getFileType(),
        uploaded.bucketName(),
        uploaded.storagePath(),
        uploaded.publicUrl(),
        sourceFile.getOriginalFilename(),
        uploaded.storedFilename(),
        uploaded.mimeType(),
        uploaded.fileSize(),
        sourceFile.getWidth(),
        sourceFile.getHeight(),
        sourceFile.getDurationSeconds());
  }

  private String resolveLibraryDirectory(Long userId, SavedAsset savedAsset) {
    String mediaDirectory = ASSET_TYPE_VIDEO.equals(savedAsset.getAssetType()) ? "videos" : "images";

    return "users/"
        + userId
        + "/library/projects/"
        + savedAsset.getLibraryProjectId()
        + "/"
        + mediaDirectory;
  }

  private LibraryProject findOwnedLibraryProject(Long userId, Long libraryProjectId) {
    LibraryProject project =
        libraryProjectRepository
            .findById(libraryProjectId)
            .orElseThrow(() -> new BusinessException(ErrorCode.STORAGE_FOLDER_NOT_FOUND));

    if (project.isDeleted() || !project.isOwnedBy(userId)) {
      throw new BusinessException(ErrorCode.STORAGE_FOLDER_NOT_FOUND);
    }

    return project;
  }

  private SavedAsset findOwnedSavedAsset(Long userId, Long savedAssetId) {
    SavedAsset savedAsset =
        savedAssetRepository
            .findById(savedAssetId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SAVED_ASSET_NOT_FOUND));

    if (savedAsset.isDeleted() || !savedAsset.isOwnedBy(userId)) {
      throw new BusinessException(ErrorCode.SAVED_ASSET_NOT_FOUND);
    }

    return savedAsset;
  }

  private GeneratedAsset findOwnedGeneratedAsset(Long userId, Long generatedAssetId) {
    GeneratedAsset generatedAsset =
        generatedAssetRepository
            .findById(generatedAssetId)
            .orElseThrow(() -> new BusinessException(ErrorCode.GENERATION_NOT_FOUND));

    if (generatedAsset.isDeleted() || !generatedAsset.isOwnedBy(userId)) {
      throw new BusinessException(ErrorCode.GENERATION_NOT_FOUND);
    }

    return generatedAsset;
  }

  private ChatMessageFile findOwnedChatMessageFile(Long userId, Long chatMessageFileId) {
    if (chatMessageFileId == null) {
      throw new BusinessException(ErrorCode.CHAT_FILE_NOT_FOUND);
    }

    ChatMessageFile file =
        chatMessageFileRepository
            .findById(chatMessageFileId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_FILE_NOT_FOUND));
    ChatMessage message = findActiveMessage(file.getMessageId());
    Chat chat =
        chatRepository
            .findById(message.getChatId())
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_FILE_NOT_FOUND));

    if (chat.isDeleted() || !chat.isOwnedBy(userId)) {
      throw new BusinessException(ErrorCode.CHAT_FILE_NOT_FOUND);
    }

    return file;
  }

  private ChatMessage findActiveMessage(Long messageId) {
    ChatMessage message =
        chatMessageRepository
            .findById(messageId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_FILE_NOT_FOUND));

    if (message.isDeleted()) {
      throw new BusinessException(ErrorCode.CHAT_FILE_NOT_FOUND);
    }

    return message;
  }

  private LibraryProjectResponse toLibraryProjectResponse(LibraryProject project) {
    return new LibraryProjectResponse(
        project.getId(),
        project.getUserId(),
        project.getParentProjectId(),
        project.getName(),
        project.getDepth(),
        project.getStatus(),
        project.getCreatedAt(),
        project.getUpdatedAt());
  }

  private SavedAssetResponse toSavedAssetResponse(
      SavedAsset savedAsset, List<SavedAssetFile> files) {
    return new SavedAssetResponse(
        savedAsset.getId(),
        savedAsset.getUserId(),
        savedAsset.getLibraryProjectId(),
        savedAsset.getSourceGeneratedAssetId(),
        savedAsset.getSourceType(),
        savedAsset.getSourceChatId(),
        savedAsset.getSourceMessageId(),
        savedAsset.getSourceMessageFileId(),
        savedAsset.getDisplayName(),
        savedAsset.getAssetType(),
        savedAsset.getStatus(),
        files.stream().map(this::toSavedAssetFileResponse).toList(),
        savedAsset.getCreatedAt());
  }

  private LibraryAssetResponse toLibraryAssetResponse(
      SavedAsset savedAsset,
      LibraryProjectSummaryResponse libraryProject,
      List<SavedAssetFile> files) {
    GeneratedAsset sourceGeneratedAsset = findSourceGeneratedAsset(savedAsset);
    Chat sourceChat = findSourceChat(sourceGeneratedAsset);
    if (sourceChat == null) {
      sourceChat = findSourceChat(savedAsset);
    }
    ChatMessage sourceMessage = findSourceMessage(savedAsset, sourceGeneratedAsset);

    return new LibraryAssetResponse(
        savedAsset.getId(),
        savedAsset.getUserId(),
        savedAsset.getDisplayName(),
        savedAsset.getAssetType(),
        savedAsset.getStatus(),
        savedAsset.getCreatedAt(),
        libraryProject,
        toLibrarySourceChatResponse(sourceChat),
        toLibrarySourceMessageResponse(sourceMessage),
        toLibrarySourceGeneratedAssetResponse(sourceGeneratedAsset),
        files.stream().map(this::toSavedAssetFileResponse).toList());
  }

  private LibraryProjectSummaryResponse toLibraryProjectSummaryResponse(LibraryProject project) {
    return new LibraryProjectSummaryResponse(
        project.getId(), project.getName(), project.getParentProjectId(), project.getDepth());
  }

  private LibrarySourceChatResponse toLibrarySourceChatResponse(Chat chat) {
    if (chat == null) {
      return null;
    }

    return new LibrarySourceChatResponse(chat.getId(), chat.getProjectId(), chat.getTitle());
  }

  private LibrarySourceMessageResponse toLibrarySourceMessageResponse(ChatMessage message) {
    if (message == null) {
      return null;
    }

    return new LibrarySourceMessageResponse(
        message.getId(),
        message.getSenderType(),
        message.getMessageType(),
        message.getContentText(),
        message.getCreatedAt());
  }

  private LibrarySourceGeneratedAssetResponse toLibrarySourceGeneratedAssetResponse(
      GeneratedAsset asset) {
    if (asset == null) {
      return null;
    }

    return new LibrarySourceGeneratedAssetResponse(
        asset.getId(),
        asset.getTitle(),
        asset.getPrompt(),
        asset.getAssetType(),
        asset.getImageCategory(),
        asset.getCreatedAt());
  }

  private GeneratedAsset findSourceGeneratedAsset(SavedAsset savedAsset) {
    if (savedAsset.getSourceGeneratedAssetId() == null) {
      return null;
    }

    return generatedAssetRepository.findById(savedAsset.getSourceGeneratedAssetId()).orElse(null);
  }

  private Chat findSourceChat(GeneratedAsset generatedAsset) {
    if (generatedAsset == null || generatedAsset.getChatId() == null) {
      return null;
    }

    return chatRepository.findById(generatedAsset.getChatId()).orElse(null);
  }

  private Chat findSourceChat(SavedAsset savedAsset) {
    if (savedAsset.getSourceChatId() == null) {
      return null;
    }

    return chatRepository.findById(savedAsset.getSourceChatId()).orElse(null);
  }

  private ChatMessage findSourceMessage(SavedAsset savedAsset, GeneratedAsset generatedAsset) {
    Long sourceMessageId = savedAsset.getSourceMessageId();
    if (sourceMessageId == null && generatedAsset != null) {
      sourceMessageId = generatedAsset.getResponseMessageId();
    }

    if (sourceMessageId == null) {
      return null;
    }

    return chatMessageRepository.findById(sourceMessageId).orElse(null);
  }

  private SavedAssetFileResponse toSavedAssetFileResponse(SavedAssetFile file) {
    return new SavedAssetFileResponse(
        file.getId(),
        file.getSavedAssetId(),
        file.getSourceAssetFileId(),
        file.getFileType(),
        file.getPublicUrl(),
        file.getOriginalFilename(),
        file.getStoredFilename(),
        file.getMimeType(),
        file.getFileSize(),
        file.getWidth(),
        file.getHeight(),
        file.getDurationSeconds());
  }

  private String resolveDisplayName(String displayName, GeneratedAsset generatedAsset) {
    if (StringUtils.hasText(displayName)) {
      return displayName.trim();
    }

    if (StringUtils.hasText(generatedAsset.getTitle())) {
      return generatedAsset.getTitle().trim();
    }

    return "저장된 에셋";
  }

  private String resolveDisplayName(String displayName, ChatMessageFile sourceFile) {
    if (StringUtils.hasText(displayName)) {
      return displayName.trim();
    }

    if (StringUtils.hasText(sourceFile.getOriginalFilename())) {
      return sourceFile.getOriginalFilename().trim();
    }

    if (StringUtils.hasText(sourceFile.getStoredFilename())) {
      return sourceFile.getStoredFilename().trim();
    }

    return "저장된 파일";
  }

  private String resolveContentType(String storageContentType, String dbContentType) {
    if (StringUtils.hasText(storageContentType)) {
      return storageContentType;
    }

    if (StringUtils.hasText(dbContentType)) {
      return dbContentType;
    }

    return "application/octet-stream";
  }

  private String normalizeAssetType(String assetType) {
    String normalized = normalizeNullableUpper(assetType);
    if (!ASSET_TYPE_IMAGE.equals(normalized) && !ASSET_TYPE_VIDEO.equals(normalized)) {
      throw new BusinessException(ErrorCode.INVALID_INPUT);
    }

    return normalized;
  }

  private String normalizeFileAssetType(String fileType, String mimeType) {
    String normalizedFileType = normalizeNullableUpper(fileType);
    if (ASSET_TYPE_IMAGE.equals(normalizedFileType) || ASSET_TYPE_VIDEO.equals(normalizedFileType)) {
      return normalizedFileType;
    }

    String normalizedMimeType = normalizeNullable(mimeType);
    if (normalizedMimeType != null) {
      String lowerMimeType = normalizedMimeType.toLowerCase();
      if (lowerMimeType.startsWith("image/")) {
        return ASSET_TYPE_IMAGE;
      }

      if (lowerMimeType.startsWith("video/")) {
        return ASSET_TYPE_VIDEO;
      }
    }

    throw new BusinessException(ErrorCode.INVALID_INPUT);
  }

  private String resolveSourceType(SaveAssetRequest request) {
    String sourceType = normalizeNullableUpper(request.sourceType());
    if (sourceType == null) {
      if (request.chatMessageFileId() != null) {
        return SOURCE_TYPE_CHAT_MESSAGE_FILE;
      }

      if (request.generatedAssetId() != null) {
        return SOURCE_TYPE_GENERATED_ASSET;
      }
    }

    if (SOURCE_TYPE_GENERATED_ASSET.equals(sourceType)) {
      if (request.generatedAssetId() == null) {
        throw new BusinessException(ErrorCode.INVALID_INPUT);
      }
      return sourceType;
    }

    if (SOURCE_TYPE_CHAT_MESSAGE_FILE.equals(sourceType)) {
      if (request.chatMessageFileId() == null) {
        throw new BusinessException(ErrorCode.INVALID_INPUT);
      }
      return sourceType;
    }

    throw new BusinessException(ErrorCode.INVALID_INPUT);
  }

  private String normalizeRequired(String value) {
    if (!StringUtils.hasText(value)) {
      throw new BusinessException(ErrorCode.INVALID_INPUT);
    }

    return value.trim();
  }

  private String normalizeNullable(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }

    return value.trim();
  }

  private String normalizeNullableUpper(String value) {
    String normalized = normalizeNullable(value);
    return normalized == null ? null : normalized.toUpperCase();
  }
}
