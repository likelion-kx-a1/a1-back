package com.likelion.a1.user.infrastructure.persistence;

import com.likelion.a1.user.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataUserRepository extends JpaRepository<User, Long> {
  boolean existsByEmailIgnoreCase(String email);
}
