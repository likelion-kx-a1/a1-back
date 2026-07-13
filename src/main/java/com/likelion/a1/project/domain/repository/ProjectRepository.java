package com.likelion.a1.project.domain.repository;

import com.likelion.a1.project.domain.model.Project;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository {
  Project save(Project project);

  Optional<Project> findById(Long id);

  List<Project> findActiveByUserId(Long userId);
}
