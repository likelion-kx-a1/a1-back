package com.likelion.a1.media.domain.repository;

import com.likelion.a1.media.domain.model.GeneratedAsset;
import java.util.Optional;

public interface GeneratedAssetRepository {
  GeneratedAsset save(GeneratedAsset asset);

  Optional<GeneratedAsset> findById(Long id);
}
