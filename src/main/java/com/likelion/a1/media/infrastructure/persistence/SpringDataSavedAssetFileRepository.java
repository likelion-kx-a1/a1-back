package com.likelion.a1.media.infrastructure.persistence;

import com.likelion.a1.media.domain.model.SavedAssetFile;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataSavedAssetFileRepository extends JpaRepository<SavedAssetFile, Long> {
  List<SavedAssetFile> findBySavedAssetIdOrderByIdAsc(Long savedAssetId);

  List<SavedAssetFile> findBySavedAssetIdInOrderByIdAsc(Collection<Long> savedAssetIds);
}
