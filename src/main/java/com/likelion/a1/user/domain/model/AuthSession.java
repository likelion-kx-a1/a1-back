package com.likelion.a1.user.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "auth_sessions")
public class AuthSession {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long userId;

  @Column(nullable = false, unique = true)
  private String sessionId;

  private String refreshTokenHash;

  private String ipAddress;

  @Column(columnDefinition = "text")
  private String userAgent;

  @Column(nullable = false)
  private boolean rememberMe;

  @Column(nullable = false)
  private OffsetDateTime expiredAt;

  private OffsetDateTime revokedAt;

  @Column(nullable = false)
  private String status;

  @Column(nullable = false)
  private OffsetDateTime createdAt;

  @Column(nullable = false)
  private OffsetDateTime updatedAt;
}
