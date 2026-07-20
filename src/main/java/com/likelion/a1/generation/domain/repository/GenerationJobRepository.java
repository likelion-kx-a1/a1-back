package com.likelion.a1.generation.domain.repository;

import com.likelion.a1.generation.domain.model.GenerationJob;
import java.util.List;
import java.util.Optional;

public interface GenerationJobRepository {
  GenerationJob save(GenerationJob job);

  Optional<GenerationJob> findById(Long id);

  List<GenerationJob> findByStatusIn(List<String> statuses);
}
