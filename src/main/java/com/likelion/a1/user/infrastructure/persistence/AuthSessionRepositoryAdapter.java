package com.likelion.a1.user.infrastructure.persistence;

import com.likelion.a1.user.domain.model.AuthSession;
import com.likelion.a1.user.domain.repository.AuthSessionRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;


@Repository
public class AuthSessionRepositoryAdapter implements AuthSessionRepository {
    private final SpringDataAuthSessionRepository repository;

    public AuthSessionRepositoryAdapter(SpringDataAuthSessionRepository repository) {
        this.repository = repository;
    }

    @Override
    public AuthSession save(AuthSession authSession) {
        return repository.save(authSession);
    }

    @Override
    public Optional<AuthSession> findBySessionId(String sessionId) {
        return repository.findBySessionId(sessionId);
    }

    @Override
    public Optional<AuthSession> findByRefreshTokenHash(String refreshTokenHash) {
        return repository.findByRefreshTokenHash(refreshTokenHash);
    }

    @Override
    public void revokeAllByUserId(Long userId) {
        repository.findAllByUserIdAndStatus(userId, "ACTIVE")
                .forEach(AuthSession::revoke);
    }
    
}
