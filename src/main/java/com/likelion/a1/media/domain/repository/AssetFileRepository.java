package com.likelion.a1.media.domain.repository;

import com.likelion.a1.media.domain.model.AssetFile;
import java.util.List;

public interface AssetFileRepository {
  List<AssetFile> saveAll(List<AssetFile> files);

  List<AssetFile> findByGeneratedAssetId(Long generatedAssetId);
}
