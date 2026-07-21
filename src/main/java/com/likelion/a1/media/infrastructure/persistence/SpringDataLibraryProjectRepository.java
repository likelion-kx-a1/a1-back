package com.likelion.a1.media.infrastructure.persistence;

import com.likelion.a1.media.domain.model.LibraryProject;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataLibraryProjectRepository extends JpaRepository<LibraryProject, Long> {
  List<LibraryProject> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

  List<LibraryProject> findByUserIdAndParentProjectIdAndStatusOrderByCreatedAtDesc(
      Long userId, Long parentProjectId, String status);

  Optional<LibraryProject> findByUserIdAndSourceProjectIdAndStatus(
      Long userId, Long sourceProjectId, String status);
}
