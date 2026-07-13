package com.likelion.a1.project.infrastructure.persistence;

import com.likelion.a1.project.domain.model.Project;
import com.likelion.a1.project.domain.repository.ProjectRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ProjectRepositoryAdapter implements ProjectRepository {
  private final SpringDataProjectRepository repository;

  public ProjectRepositoryAdapter(SpringDataProjectRepository repository) {
    this.repository = repository;
  }

  @Override
  public Project save(Project project) {
    return repository.save(project);
  }

  @Override
  public Optional<Project> findById(Long id) {
    return repository.findById(id);
  }

  @Override
  public List<Project> findActiveByUserId(Long userId) {
    return repository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
  }
}
