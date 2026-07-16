package com.likelion.a1.media.infrastructure.persistence;

import com.likelion.a1.media.domain.model.AssetFile;
import com.likelion.a1.media.domain.repository.AssetFileRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class AssetFileRepositoryAdapter implements AssetFileRepository {
  private final SpringDataAssetFileRepository repository;

  public AssetFileRepositoryAdapter(SpringDataAssetFileRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<AssetFile> saveAll(List<AssetFile> files) {
    return repository.saveAll(files);
  }

  @Override
  public List<AssetFile> findByGeneratedAssetId(Long generatedAssetId) {
    return repository.findByGeneratedAssetIdOrderByIdAsc(generatedAssetId);
  }
}
