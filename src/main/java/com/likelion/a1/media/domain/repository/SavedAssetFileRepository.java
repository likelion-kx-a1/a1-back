package com.likelion.a1.media.domain.repository;

import com.likelion.a1.media.domain.model.SavedAssetFile;
import java.util.Collection;
import java.util.List;

public interface SavedAssetFileRepository {
  List<SavedAssetFile> saveAll(List<SavedAssetFile> files);

  List<SavedAssetFile> findBySavedAssetId(Long savedAssetId);

  List<SavedAssetFile> findBySavedAssetIds(Collection<Long> savedAssetIds);
}
