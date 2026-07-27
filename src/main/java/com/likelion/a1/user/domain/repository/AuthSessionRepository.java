package com.likelion.a1.user.domain.repository;

import com.likelion.a1.user.domain.model.AuthSession;
import java.util.Optional;

public interface AuthSessionRepository {
    AuthSession save(AuthSession authSession);

    Optional<AuthSession> findBySessionId(String sessionId);
    Optional<AuthSession> findByRefreshTokenHash(String refreshTokenHash);

    void revokeAllByUserId(Long userId);

    /** 비밀번호 변경 시 현재 세션은 살려두고 다른 기기의 세션만 강제 종료하기 위해 쓴다. */
    void revokeAllByUserIdExceptSessionId(Long userId, String exceptSessionId);
}
