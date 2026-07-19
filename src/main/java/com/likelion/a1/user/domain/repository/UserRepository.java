package com.likelion.a1.user.domain.repository;

import com.likelion.a1.user.domain.model.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepository {
  User save(User user);

  Optional<User> findById(Long id);
  Optional<User> findByLoginId(String loginId);
  Optional<User> findByEmail(String email);

  boolean existsByLoginId(String loginId);
  boolean existsByEmail(String email);

  Page<User> findSignupRequests(Pageable pageable);

  Page<User> searchUsers(
      String approvalStatus, String accountStatus, String keyword, Pageable pageable);
}
