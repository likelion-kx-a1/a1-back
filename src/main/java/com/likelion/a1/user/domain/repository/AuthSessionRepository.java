package com.likelion.a1.user.domain.repository;

import com.likelion.a1.user.domain.model.AuthSession;
import java.util.Optional;

public interface AuthSessionRepository {
    AuthSession save(AuthSession authSession);

    Optional<AuthSession> findBySessionId(String sessionId);
    Optional<AuthSession> findByRefreshTokenHash(String refreshTokenHash);

    void revokeAllByUserId(Long userId);

    
}
