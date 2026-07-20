package com.likelion.a1.media.domain.repository;

import com.likelion.a1.media.domain.model.StorageFolder;
import java.util.List;
import java.util.Optional;

public interface StorageFolderRepository {
  StorageFolder save(StorageFolder folder);

  Optional<StorageFolder> findById(Long id);

  List<StorageFolder> findActiveByUserId(Long userId);

  List<StorageFolder> findActiveByLibraryProjectId(Long userId, Long libraryProjectId);

  List<StorageFolder> findActiveByLibraryProjectIdAndParentFolderId(
      Long userId, Long libraryProjectId, Long parentFolderId);

  Optional<StorageFolder> findActiveSystemFolder(
      Long userId, Long libraryProjectId, String assetType);

  boolean existsActiveChild(Long parentFolderId);
}
