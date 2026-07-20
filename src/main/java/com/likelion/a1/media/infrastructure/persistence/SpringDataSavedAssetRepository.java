package com.likelion.a1.media.infrastructure.persistence;

import com.likelion.a1.media.domain.model.SavedAsset;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataSavedAssetRepository extends JpaRepository<SavedAsset, Long> {
  @Query(
      """
      select savedAsset
      from SavedAsset savedAsset
      where savedAsset.userId = :userId
        and savedAsset.status = 'ACTIVE'
        and (:libraryProjectId is null or savedAsset.libraryProjectId = :libraryProjectId)
        and (:folderId is null or savedAsset.folderId = :folderId)
        and (:assetType is null or savedAsset.assetType = :assetType)
        and (
          :keyword is null
          or lower(savedAsset.displayName) like lower(concat('%', :keyword, '%'))
        )
      order by savedAsset.createdAt desc
      """)
  List<SavedAsset> findActiveByUserId(
      @Param("userId") Long userId,
      @Param("libraryProjectId") Long libraryProjectId,
      @Param("folderId") Long folderId,
      @Param("assetType") String assetType,
      @Param("keyword") String keyword);
}
