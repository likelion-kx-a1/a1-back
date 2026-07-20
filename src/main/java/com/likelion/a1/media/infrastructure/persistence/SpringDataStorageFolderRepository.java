package com.likelion.a1.media.infrastructure.persistence;

import com.likelion.a1.media.domain.model.StorageFolder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataStorageFolderRepository extends JpaRepository<StorageFolder, Long> {
  List<StorageFolder> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

  List<StorageFolder> findByUserIdAndLibraryProjectIdAndStatusOrderByCreatedAtDesc(
      Long userId, Long libraryProjectId, String status);

  List<StorageFolder>
      findByUserIdAndLibraryProjectIdAndParentFolderIdAndStatusOrderByCreatedAtDesc(
          Long userId, Long libraryProjectId, Long parentFolderId, String status);

  Optional<StorageFolder>
      findByUserIdAndLibraryProjectIdAndFolderTypeAndAssetTypeAndStatus(
          Long userId, Long libraryProjectId, String folderType, String assetType, String status);

  boolean existsByParentFolderIdAndStatus(Long parentFolderId, String status);
}
