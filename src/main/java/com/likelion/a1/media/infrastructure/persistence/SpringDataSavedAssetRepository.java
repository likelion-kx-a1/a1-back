package com.likelion.a1.media.infrastructure.persistence;

import com.likelion.a1.media.domain.model.SavedAsset;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataSavedAssetRepository extends JpaRepository<SavedAsset, Long> {
  List<SavedAsset> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);
}
