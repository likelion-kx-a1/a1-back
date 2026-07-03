package com.likelion.a1.user.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity @Table(name = "auth_sessions")
public class AuthSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long userId;
    @Column(nullable=false, unique=true) private String sessionId;
    @Column(nullable=false) private String refreshTokenHash;
    private String deviceName; private String deviceType; private String browser; private String operatingSystem;
    @Column(columnDefinition="inet") private String ipAddress;
    @Column(columnDefinition="text") private String userAgent;
    @Column(nullable=false) private boolean rememberMe;
    private OffsetDateTime accessTokenExpiredAt;
    @Column(nullable=false) private OffsetDateTime refreshTokenExpiredAt;
    private OffsetDateTime lastAccessedAt; private OffsetDateTime revokedAt;
    @Column(nullable=false) private String status;
    @Column(nullable=false) private OffsetDateTime createdAt;
    @Column(nullable=false) private OffsetDateTime updatedAt;
}
