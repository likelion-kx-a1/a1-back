package com.likelion.a1.media.domain.repository;

import com.likelion.a1.media.domain.model.LibraryProject;
import java.util.List;
import java.util.Optional;

public interface LibraryProjectRepository {
  LibraryProject save(LibraryProject project);

  Optional<LibraryProject> findById(Long id);

  List<LibraryProject> findActiveByUserId(Long userId);

  List<LibraryProject> findActiveByUserIdAndParentProjectId(Long userId, Long parentProjectId);
}
