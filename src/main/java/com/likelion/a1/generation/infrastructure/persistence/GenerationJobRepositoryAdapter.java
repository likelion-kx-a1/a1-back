package com.likelion.a1.generation.infrastructure.persistence;

import com.likelion.a1.generation.domain.model.GenerationJob;
import com.likelion.a1.generation.domain.repository.GenerationJobRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class GenerationJobRepositoryAdapter implements GenerationJobRepository {
  private final SpringDataGenerationJobRepository repository;

  GenerationJobRepositoryAdapter(SpringDataGenerationJobRepository repository) {
    this.repository = repository;
  }

  public GenerationJob save(GenerationJob job) {
    return repository.save(job);
  }

  public Optional<GenerationJob> findById(Long id) {
    return repository.findById(id);
  }

  public List<GenerationJob> findByStatusIn(List<String> statuses) {
    return repository.findByStatusIn(statuses);
  }
}
