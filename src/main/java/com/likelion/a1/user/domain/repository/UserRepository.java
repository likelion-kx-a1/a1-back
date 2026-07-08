package com.likelion.a1.user.domain.repository;

import com.likelion.a1.user.domain.model.User;
import java.util.Optional;

public interface UserRepository {
  User save(User user);

  Optional<User> findById(Long id);
  Optional<User> findByLoginId(String loginId);
  Optional<User> findByEmail(String email);

  boolean existsByLoginId(String loginId);
  boolean existsByEmail(String email);
}
