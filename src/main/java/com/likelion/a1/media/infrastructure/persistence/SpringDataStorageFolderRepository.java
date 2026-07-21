package com.likelion.a1.media.infrastructure.persistence;

import com.likelion.a1.media.domain.model.StorageFolder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataStorageFolderRepository extends JpaRepository<StorageFolder, Long> {
  List<StorageFolder> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

  List<StorageFolder> findByUserIdAndLibraryProjectIdAndStatusOrderByCreatedAtDesc(
      Long userId, Long libraryProjectId, String status);

  @Query(
      """
      select f
      from StorageFolder f
      where f.userId = :userId
        and f.libraryProjectId = :libraryProjectId
        and f.status = :status
        and (
          (:parentFolderId is null and f.parentFolderId is null)
          or f.parentFolderId = :parentFolderId
        )
      order by f.createdAt desc
      """)
  List<StorageFolder> findActiveChildren(
      @Param("userId") Long userId,
      @Param("libraryProjectId") Long libraryProjectId,
      @Param("parentFolderId") Long parentFolderId,
      @Param("status") String status);

  Optional<StorageFolder>
      findByUserIdAndLibraryProjectIdAndFolderTypeAndAssetTypeAndStatus(
          Long userId, Long libraryProjectId, String folderType, String assetType, String status);

  boolean existsByParentFolderIdAndStatus(Long parentFolderId, String status);
}
