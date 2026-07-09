package com.likelion.a1.user.infrastructure.persistence;

import com.likelion.a1.user.domain.model.AuthSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAuthSessionRepository extends JpaRepository<AuthSession, Long> {

  Optional<AuthSession> findBySessionId(String sessionId);
  
  Optional<AuthSession> findByRefreshTokenHash(String refreshTokenHash);

  List<AuthSession> findAllByUserIdAndStatus(Long userId, String status);
}
