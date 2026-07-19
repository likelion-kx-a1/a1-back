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

  @Column(nullable = false)
  private String refreshTokenHash;

  @Column(length = 50)
  private String ipAddress;

  @Column(columnDefinition = "text")
  private String userAgent;

  @Column(nullable = false)
  private OffsetDateTime expiredAt;
  
  @Column
  private OffsetDateTime revokedAt;

  @Column(nullable = false, length = 30)
  private String status = "ACTIVE";

  @Column(nullable = false)
  private OffsetDateTime createdAt;

  @Column(nullable = false)
  private OffsetDateTime updatedAt;

  public static AuthSession create(
      Long userId,
      String sessionId,
      String refreshTokenHash,
      String ipAddress,
      String userAgent,
      OffsetDateTime expiredAt) {
    AuthSession session = new AuthSession();
    OffsetDateTime now = OffsetDateTime.now();
    
    // Set the properties of the session object
    session.userId = userId;
    session.sessionId = sessionId;
    session.refreshTokenHash = refreshTokenHash;
    session.ipAddress = ipAddress;
    session.userAgent = userAgent;
    session.expiredAt = expiredAt;
    session.status = "ACTIVE";
    session.createdAt = now;
    session.updatedAt = now;

    return session;
  }
  
  public void revoke() {
    this.status = "REVOKED";
    this.revokedAt = OffsetDateTime.now();
    this.updatedAt = this.revokedAt;
  }

  public boolean isActive() {
    return "ACTIVE".equals(this.status)
        && this.revokedAt == null
        && this.expiredAt.isAfter(OffsetDateTime.now());
  }

  public boolean isOwnedBy(Long userId, String sessionId) {
    return this.userId.equals(userId) && this.sessionId.equals(sessionId);
  }
}
