package com.likelion.a1.user.infrastructure.security;

public record JwtPrincipal(
    Long userId,
    String loginId,
    String role,
    String sessionId
) {}
