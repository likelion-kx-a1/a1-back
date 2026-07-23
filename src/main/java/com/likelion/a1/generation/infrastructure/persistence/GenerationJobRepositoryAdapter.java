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

  /**
   * saveAndFlush를 써서 @Version 낙관적 락 충돌(ObjectOptimisticLockingFailureException)이 이 호출
   * 시점에 즉시 터지도록 한다. 일반 save()는 트랜잭션 커밋 시점까지 flush를 미룰 수 있어, 여러 job을
   * 순회하며 저장하는 {@link com.likelion.a1.generation.application.service.GenerationVideoPollingScheduler}에서
   * 충돌이 나면 배치 전체가 롤백되기 전까지 어느 job이 문제였는지 알 수 없게 된다.
   */
  public GenerationJob save(GenerationJob job) {
    GenerationJob saved = repository.saveAndFlush(job);
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
