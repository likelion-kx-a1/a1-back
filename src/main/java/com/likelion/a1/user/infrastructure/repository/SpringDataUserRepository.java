package com.likelion.a1.user.infrastructure.repository;

import com.likelion.a1.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataUserRepository extends JpaRepository<User, Long> {
    boolean existsByEmailIgnoreCase(String email);
}
