package com.likelion.a1.generation.infrastructure.persistence;

import com.likelion.a1.generation.domain.event.GenerationCompletedEvent;
import com.likelion.a1.generation.domain.model.GenerationJob;
import com.likelion.a1.generation.domain.model.GenerationStatus;
import com.likelion.a1.generation.domain.repository.GenerationJobRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;

@Repository
class GenerationJobRepositoryAdapter implements GenerationJobRepository {
  private final SpringDataGenerationJobRepository repository;
  private final ApplicationEventPublisher eventPublisher;

  GenerationJobRepositoryAdapter(
      SpringDataGenerationJobRepository repository, ApplicationEventPublisher eventPublisher) {
    this.repository = repository;
    this.eventPublisher = eventPublisher;
  }

  public GenerationJob save(GenerationJob job) {
    GenerationJob saved = repository.save(job);
    if (isTerminal(saved.getStatus())) {
      eventPublisher.publishEvent(GenerationCompletedEvent.from(saved));
    }
    return saved;
  }

  private boolean isTerminal(String status) {
    return GenerationStatus.COMPLETED.name().equals(status) || GenerationStatus.FAILED.name().equals(status);
  }

  public Optional<GenerationJob> findById(Long id) {
    return repository.findById(id);
  }

  public List<GenerationJob> findByStatusIn(List<String> statuses) {
    return repository.findByStatusIn(statuses);
  }
}
