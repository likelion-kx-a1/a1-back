package com.likelion.a1.media.infrastructure.persistence;

import com.likelion.a1.media.domain.model.SavedAssetFile;
import com.likelion.a1.media.domain.repository.SavedAssetFileRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class SavedAssetFileRepositoryAdapter implements SavedAssetFileRepository {
  private final SpringDataSavedAssetFileRepository repository;

  public SavedAssetFileRepositoryAdapter(SpringDataSavedAssetFileRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<SavedAssetFile> saveAll(List<SavedAssetFile> files) {
    return repository.saveAll(files);
  }

  @Override
  public List<SavedAssetFile> findBySavedAssetId(Long savedAssetId) {
    return repository.findBySavedAssetIdOrderByIdAsc(savedAssetId);
  }

  @Override
  public List<SavedAssetFile> findBySavedAssetIds(Collection<Long> savedAssetIds) {
    if (savedAssetIds == null || savedAssetIds.isEmpty()) {
      return List.of();
    }

    return repository.findBySavedAssetIdInOrderByIdAsc(savedAssetIds);
  }
}
