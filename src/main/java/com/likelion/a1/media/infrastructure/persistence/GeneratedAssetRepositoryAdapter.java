package com.likelion.a1.media.infrastructure.persistence;

import com.likelion.a1.media.domain.model.GeneratedAsset;
import com.likelion.a1.media.domain.repository.GeneratedAssetRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class GeneratedAssetRepositoryAdapter implements GeneratedAssetRepository {
  private final SpringDataGeneratedAssetRepository repository;

  public GeneratedAssetRepositoryAdapter(SpringDataGeneratedAssetRepository repository) {
    this.repository = repository;
  }

  @Override
  public GeneratedAsset save(GeneratedAsset asset) {
    return repository.save(asset);
  }

  @Override
  public Optional<GeneratedAsset> findById(Long id) {
    return repository.findById(id);
  }
}
