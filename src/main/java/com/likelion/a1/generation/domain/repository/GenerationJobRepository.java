package com.likelion.a1.generation.domain.repository;

import com.likelion.a1.generation.domain.GenerationJob;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface GenerationJobRepository {
    GenerationJob save(GenerationJob job);
    Optional<GenerationJob> findById(Long id);
    Optional<GenerationJob> findByPublicId(UUID publicId);
    List<GenerationJob> findProcessingVideos();
}
