package com.likelion.a1.media.infrastructure.persistence;

import com.likelion.a1.media.domain.model.SavedAsset;
import com.likelion.a1.media.domain.repository.SavedAssetRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class SavedAssetRepositoryAdapter implements SavedAssetRepository {
  private final SpringDataSavedAssetRepository repository;

  public SavedAssetRepositoryAdapter(SpringDataSavedAssetRepository repository) {
    this.repository = repository;
  }

  @Override
  public SavedAsset save(SavedAsset savedAsset) {
    return repository.save(savedAsset);
  }

  @Override
  public Optional<SavedAsset> findById(Long id) {
    return repository.findById(id);
  }

  @Override
  public List<SavedAsset> findActiveByUserId(
      Long userId, Long libraryProjectId, Long folderId, String assetType, String keyword) {
    return repository.findActiveByUserId(userId, libraryProjectId, folderId, assetType, keyword);
  }
}
