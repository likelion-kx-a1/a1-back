package com.likelion.a1.project.infrastructure.persistence;

import com.likelion.a1.project.domain.model.Project;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataProjectRepository extends JpaRepository<Project, Long> {
  List<Project> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);
}
