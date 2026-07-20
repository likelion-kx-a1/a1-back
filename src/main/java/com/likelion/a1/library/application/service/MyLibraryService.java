package com.likelion.a1.library.application.service;

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
import com.likelion.a1.media.domain.model.StorageFolder;
import com.likelion.a1.media.domain.repository.AssetFileRepository;
import com.likelion.a1.media.domain.repository.GeneratedAssetRepository;
import com.likelion.a1.media.domain.repository.LibraryProjectRepository;
import com.likelion.a1.media.domain.repository.SavedAssetFileRepository;
import com.likelion.a1.media.domain.repository.SavedAssetRepository;
import com.likelion.a1.media.domain.repository.StorageFolderRepository;
import com.likelion.a1.media.presentation.dto.MediaDtos.CreateLibraryProjectRequest;
import com.likelion.a1.media.presentation.dto.MediaDtos.LibraryProjectResponse;
import com.likelion.a1.media.presentation.dto.MediaDtos.SaveAssetRequest;
import com.likelion.a1.media.presentation.dto.MediaDtos.SavedAssetFileResponse;
import com.likelion.a1.media.presentation.dto.MediaDtos.SavedAssetResponse;
import com.likelion.a1.media.presentation.dto.MediaDtos.StorageFolderResponse;
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

  private final LibraryProjectRepository libraryProjectRepository;
  private final StorageFolderRepository folderRepository;
  private final SavedAssetRepository savedAssetRepository;
  private final SavedAssetFileRepository savedAssetFileRepository;
  private final GeneratedAssetRepository generatedAssetRepository;
  private final AssetFileRepository assetFileRepository;
  private final MediaStoragePort mediaStoragePort;

  public MyLibraryService(
      LibraryProjectRepository libraryProjectRepository,
      StorageFolderRepository folderRepository,
      SavedAssetRepository savedAssetRepository,
      SavedAssetFileRepository savedAssetFileRepository,
      GeneratedAssetRepository generatedAssetRepository,
      AssetFileRepository assetFileRepository,
      MediaStoragePort mediaStoragePort) {
    this.libraryProjectRepository = libraryProjectRepository;
    this.folderRepository = folderRepository;
    this.savedAssetRepository = savedAssetRepository;
    this.savedAssetFileRepository = savedAssetFileRepository;
    this.generatedAssetRepository = generatedAssetRepository;
    this.assetFileRepository = assetFileRepository;
    this.mediaStoragePort = mediaStoragePort;
  }

  public LibraryProjectResponse createLibraryProject(
      Long userId, CreateLibraryProjectRequest request) {
    Long parentProjectId = request.parentProjectId();
    int depth = 0;
    if (parentProjectId != null) {
      LibraryProject parent = findOwnedLibraryProject(userId, parentProjectId);
      if (parent.getDepth() >= 1) {
        throw new BusinessException(ErrorCode.INVALID_STORAGE_FOLDER_DEPTH);
      }
      depth = parent.getDepth() + 1;
    }

    LibraryProject project =
        libraryProjectRepository.save(
            LibraryProject.create(userId, parentProjectId, normalizeRequired(request.name()), depth));
    StorageFolder imageFolder = createSystemFolder(userId, project.getId(), ASSET_TYPE_IMAGE);
    StorageFolder videoFolder = createSystemFolder(userId, project.getId(), ASSET_TYPE_VIDEO);

    return toLibraryProjectResponse(project, imageFolder, videoFolder);
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

  @Transactional(readOnly = true)
  public List<StorageFolderResponse> getFolders(Long userId, Long libraryProjectId) {
    findOwnedLibraryProject(userId, libraryProjectId);

    return folderRepository.findActiveByLibraryProjectId(userId, libraryProjectId).stream()
        .map(this::toFolderResponse)
        .toList();
  }

  public SavedAssetResponse saveAsset(Long userId, SaveAssetRequest request) {
    LibraryProject project = findOwnedLibraryProject(userId, request.libraryProjectId());
    GeneratedAsset generatedAsset = findOwnedGeneratedAsset(userId, request.generatedAssetId());
    String assetType = normalizeAssetType(generatedAsset.getAssetType());
    StorageFolder targetFolder = findSystemFolder(userId, project.getId(), assetType);

    SavedAsset savedAsset =
        savedAssetRepository.save(
            SavedAsset.create(
                userId,
                project.getId(),
                targetFolder.getId(),
                generatedAsset.getId(),
                assetType,
                resolveDisplayName(request.displayName(), generatedAsset)));

    List<SavedAssetFile> copiedFiles = copyGeneratedAssetFiles(userId, savedAsset);
    savedAssetFileRepository.saveAll(copiedFiles);

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
            userId, libraryProjectId, null, normalizeNullableUpper(assetType), normalizeNullable(keyword));
    Map<Long, List<SavedAssetFile>> filesBySavedAssetId =
        savedAssetFileRepository.findBySavedAssetIds(savedAssets.stream().map(SavedAsset::getId).toList())
            .stream()
            .collect(Collectors.groupingBy(SavedAssetFile::getSavedAssetId));

    return savedAssets.stream()
        .map(savedAsset -> toSavedAssetResponse(savedAsset, filesBySavedAssetId.getOrDefault(savedAsset.getId(), List.of())))
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

    folderRepository.findActiveByLibraryProjectId(userId, libraryProjectId).stream()
        .forEach(
            folder -> {
              folder.delete();
              folderRepository.save(folder);
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

  private String resolveLibraryDirectory(Long userId, SavedAsset savedAsset) {
    String mediaDirectory = ASSET_TYPE_VIDEO.equals(savedAsset.getAssetType()) ? "videos" : "images";

    return "users/"
        + userId
        + "/library/projects/"
        + savedAsset.getLibraryProjectId()
        + "/"
        + mediaDirectory;
  }

  private StorageFolder createSystemFolder(Long userId, Long libraryProjectId, String assetType) {
    return folderRepository.save(
        StorageFolder.create(
            userId,
            libraryProjectId,
            null,
            ASSET_TYPE_VIDEO.equals(assetType) ? "동영상" : "이미지",
            "SYSTEM",
            assetType));
  }

  private StorageFolder findSystemFolder(Long userId, Long libraryProjectId, String assetType) {
    return folderRepository
        .findActiveSystemFolder(userId, libraryProjectId, assetType)
        .orElseGet(() -> createSystemFolder(userId, libraryProjectId, assetType));
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

  private LibraryProjectResponse toLibraryProjectResponse(LibraryProject project) {
    return toLibraryProjectResponse(
        project,
        findSystemFolder(project.getUserId(), project.getId(), ASSET_TYPE_IMAGE),
        findSystemFolder(project.getUserId(), project.getId(), ASSET_TYPE_VIDEO));
  }

  private LibraryProjectResponse toLibraryProjectResponse(
      LibraryProject project, StorageFolder imageFolder, StorageFolder videoFolder) {
    return new LibraryProjectResponse(
        project.getId(),
        project.getUserId(),
        project.getParentProjectId(),
        project.getName(),
        project.getDepth(),
        project.getStatus(),
        toFolderResponse(imageFolder),
        toFolderResponse(videoFolder),
        project.getCreatedAt(),
        project.getUpdatedAt());
  }

  private SavedAssetResponse toSavedAssetResponse(
      SavedAsset savedAsset, List<SavedAssetFile> files) {
    return new SavedAssetResponse(
        savedAsset.getId(),
        savedAsset.getUserId(),
        savedAsset.getLibraryProjectId(),
        savedAsset.getFolderId(),
        savedAsset.getSourceGeneratedAssetId(),
        savedAsset.getDisplayName(),
        savedAsset.getAssetType(),
        savedAsset.getStatus(),
        files.stream().map(this::toSavedAssetFileResponse).toList(),
        savedAsset.getCreatedAt());
  }

  private StorageFolderResponse toFolderResponse(StorageFolder folder) {
    return new StorageFolderResponse(
        folder.getId(),
        folder.getUserId(),
        folder.getParentFolderId(),
        folder.getLibraryProjectId(),
        folder.getName(),
        folder.getFolderType(),
        folder.getAssetType(),
        folder.getStatus(),
        folder.getCreatedAt());
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
