package com.likelion.a1.generation.infrastructure.persistence;

import com.likelion.a1.generation.domain.model.GenerationJob;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataGenerationJobRepository extends JpaRepository<GenerationJob, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select job from GenerationJob job where job.id = :id")
  Optional<GenerationJob> findByIdForUpdate(@Param("id") Long id);

  List<GenerationJob> findByStatusIn(List<String> statuses);

  Optional<GenerationJob> findFirstByChatIdAndStatusInOrderByCreatedAtDesc(
      Long chatId, List<String> statuses);
}
