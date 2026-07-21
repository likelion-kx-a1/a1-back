package com.likelion.a1.media.infrastructure.persistence;

import com.likelion.a1.media.domain.model.StorageFolder;
import com.likelion.a1.media.domain.repository.StorageFolderRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class StorageFolderRepositoryAdapter implements StorageFolderRepository {
  private final SpringDataStorageFolderRepository repository;

  public StorageFolderRepositoryAdapter(SpringDataStorageFolderRepository repository) {
    this.repository = repository;
  }

  @Override
  public StorageFolder save(StorageFolder folder) {
    return repository.save(folder);
  }

  @Override
  public Optional<StorageFolder> findById(Long id) {
    return repository.findById(id);
  }

  @Override
  public List<StorageFolder> findActiveByUserId(Long userId) {
    return repository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, "ACTIVE");
  }

  @Override
  public List<StorageFolder> findActiveByLibraryProjectId(Long userId, Long libraryProjectId) {
    return repository.findByUserIdAndLibraryProjectIdAndStatusOrderByCreatedAtDesc(
        userId, libraryProjectId, "ACTIVE");
  }

  @Override
  public List<StorageFolder> findActiveByLibraryProjectIdAndParentFolderId(
      Long userId, Long libraryProjectId, Long parentFolderId) {
    return repository.findActiveChildren(userId, libraryProjectId, parentFolderId, "ACTIVE");
  }

  @Override
  public Optional<StorageFolder> findActiveSystemFolder(
      Long userId, Long libraryProjectId, String assetType) {
    return repository.findByUserIdAndLibraryProjectIdAndFolderTypeAndAssetTypeAndStatus(
        userId, libraryProjectId, "SYSTEM", assetType, "ACTIVE");
  }

  @Override
  public boolean existsActiveChild(Long parentFolderId) {
    return repository.existsByParentFolderIdAndStatus(parentFolderId, "ACTIVE");
  }
}
