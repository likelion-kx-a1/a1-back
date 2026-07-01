package com.likelion.a1.generation.infrastructure.repository;

import com.likelion.a1.generation.domain.GenerationJob;
import com.likelion.a1.generation.domain.repository.GenerationJobRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import com.likelion.a1.generation.domain.GenerationStatus;
import com.likelion.a1.generation.domain.GenerationType;

@Repository
class GenerationJobRepositoryAdapter implements GenerationJobRepository {
    private final SpringDataGenerationJobRepository repository;

    GenerationJobRepositoryAdapter(SpringDataGenerationJobRepository repository) { this.repository = repository; }
    public GenerationJob save(GenerationJob job) { return repository.save(job); }
    public Optional<GenerationJob> findById(Long id) { return repository.findById(id); }
    public Optional<GenerationJob> findByPublicId(UUID id) { return repository.findByPublicId(id); }
    public List<GenerationJob> findProcessingVideos() {
        return repository.findTop20ByTypeAndStatusInOrderByCreatedAtAsc(
                GenerationType.VIDEO_GENERATION,
                List.of(GenerationStatus.QUEUED, GenerationStatus.PROCESSING));
    }
}
