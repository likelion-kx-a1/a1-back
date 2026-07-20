package com.likelion.a1.media.infrastructure.persistence;

import com.likelion.a1.media.domain.model.GeneratedAsset;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataGeneratedAssetRepository extends JpaRepository<GeneratedAsset, Long> {}
