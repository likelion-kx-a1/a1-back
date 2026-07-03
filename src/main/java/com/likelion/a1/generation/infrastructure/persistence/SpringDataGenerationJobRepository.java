package com.likelion.a1.generation.infrastructure.persistence;

import com.likelion.a1.generation.domain.model.GenerationJob;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataGenerationJobRepository extends JpaRepository<GenerationJob, Long> {}
