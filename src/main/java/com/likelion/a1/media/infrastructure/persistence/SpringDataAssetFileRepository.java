package com.likelion.a1.media.infrastructure.persistence;

import com.likelion.a1.media.domain.model.AssetFile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAssetFileRepository extends JpaRepository<AssetFile, Long> {
  List<AssetFile> findByGeneratedAssetIdOrderByIdAsc(Long generatedAssetId);
}
