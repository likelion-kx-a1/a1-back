package com.likelion.a1.generation.infrastructure.persistence;

import com.likelion.a1.generation.domain.GenerationJob;
import com.likelion.a1.generation.domain.GenerationStatus;
import com.likelion.a1.generation.domain.GenerationType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

interface SpringDataGenerationJobRepository extends JpaRepository<GenerationJob, Long> {
    java.util.Optional<GenerationJob> findByPublicId(UUID publicId);
    List<GenerationJob> findTop20ByTypeAndStatusInOrderByCreatedAtAsc(
            GenerationType type, List<GenerationStatus> statuses);
}
