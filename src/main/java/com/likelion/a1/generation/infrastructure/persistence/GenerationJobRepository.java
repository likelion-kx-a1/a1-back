package com.likelion.a1.generation.infrastructure.persistence;

import com.likelion.a1.generation.domain.model.GenerationJob;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GenerationJobRepository extends JpaRepository<GenerationJob, Long> {
  List<GenerationJob> findByStatusIn(List<String> statuses);
}
