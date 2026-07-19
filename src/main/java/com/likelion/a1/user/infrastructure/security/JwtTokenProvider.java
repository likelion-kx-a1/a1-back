package com.likelion.a1.user.infrastructure.security;

import com.likelion.a1.user.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {
  private final SecretKey key;
  private final long accessTokenExpirationSeconds;

  public JwtTokenProvider(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.access-token-expiration-seconds}") long accessTokenExpirationSeconds) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
  }

  public String createAccessToken(User user, String sessionId) {
    Instant now = Instant.now();

    return Jwts.builder()
        .subject(String.valueOf(user.getId()))
        .claim("loginId", user.getLoginId())
        .claim("role", user.getRole())
        .claim("sessionId", sessionId)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(accessTokenExpirationSeconds)))
        .signWith(key)
        .compact();
  }

  public JwtPrincipal parse(String token) {
    Claims claims =
        Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();

    return new JwtPrincipal(
        Long.valueOf(claims.getSubject()),
        claims.get("loginId", String.class),
        claims.get("role", String.class),
        claims.get("sessionId", String.class));
  }

  public long accessTokenExpirationSeconds() {
    return accessTokenExpirationSeconds;
  }
}