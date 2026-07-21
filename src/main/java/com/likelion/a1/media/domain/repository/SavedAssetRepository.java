package com.likelion.a1.media.domain.repository;

import com.likelion.a1.media.domain.model.SavedAsset;
import java.util.List;
import java.util.Optional;

public interface SavedAssetRepository {
  SavedAsset save(SavedAsset savedAsset);

  Optional<SavedAsset> findById(Long id);

  List<SavedAsset> findActiveByUserId(
      Long userId,
      Long libraryProjectId,
      Long folderId,
      boolean filterFolder,
      String assetType,
      String keyword);
}
