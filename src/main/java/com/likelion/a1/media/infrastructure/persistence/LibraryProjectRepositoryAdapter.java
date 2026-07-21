package com.likelion.a1.media.infrastructure.persistence;

import com.likelion.a1.media.domain.model.LibraryProject;
import com.likelion.a1.media.domain.repository.LibraryProjectRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class LibraryProjectRepositoryAdapter implements LibraryProjectRepository {
  private final SpringDataLibraryProjectRepository repository;

  public LibraryProjectRepositoryAdapter(SpringDataLibraryProjectRepository repository) {
    this.repository = repository;
  }

  @Override
  public LibraryProject save(LibraryProject project) {
    return repository.save(project);
  }

  @Override
  public Optional<LibraryProject> findById(Long id) {
    return repository.findById(id);
  }

  @Override
  public List<LibraryProject> findActiveByUserId(Long userId) {
    return repository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, "ACTIVE");
  }

  @Override
  public List<LibraryProject> findActiveByUserIdAndParentProjectId(
      Long userId, Long parentProjectId) {
    return repository.findByUserIdAndParentProjectIdAndStatusOrderByCreatedAtDesc(
        userId, parentProjectId, "ACTIVE");
  }

  @Override
  public Optional<LibraryProject> findActiveByUserIdAndSourceProjectId(
      Long userId, Long sourceProjectId) {
    return repository.findByUserIdAndSourceProjectIdAndStatus(userId, sourceProjectId, "ACTIVE");
  }
}
