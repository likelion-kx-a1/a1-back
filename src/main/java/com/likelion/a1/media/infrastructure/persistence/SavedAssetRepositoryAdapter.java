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
      Long userId,
      Long libraryProjectId,
      Long folderId,
      boolean filterFolder,
      String assetType,
      String keyword) {
    String normalizedKeyword = normalizeKeyword(keyword);

    return repository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, "ACTIVE").stream()
        .filter(
            savedAsset ->
                libraryProjectId == null || libraryProjectId.equals(savedAsset.getLibraryProjectId()))
        .filter(
            savedAsset ->
                !filterFolder
                    || (folderId == null
                        ? savedAsset.getFolderId() == null
                        : folderId.equals(savedAsset.getFolderId())))
        .filter(savedAsset -> assetType == null || assetType.equals(savedAsset.getAssetType()))
        .filter(
            savedAsset ->
                normalizedKeyword == null
                    || savedAsset.getDisplayName().toLowerCase().contains(normalizedKeyword))
        .toList();
  }

  private String normalizeKeyword(String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return null;
    }

    return keyword.trim().toLowerCase();
  }
}
