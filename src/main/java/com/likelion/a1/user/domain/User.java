package com.likelion.a1.user.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, updatable = false)
    private UUID publicId;
    @Column(nullable = false, unique = true)
    private String email;
    private String passwordHash;
    @Column(nullable = false, length = 100)
    private String nickname;
    @Column(columnDefinition = "text")
    private String profileImageUrl;
    @Column(nullable = false, length = 30)
    private String authProvider;
    private String providerId;
    @Column(nullable = false, length = 30)
    private String role;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(nullable = false)
    private int loginCount;
    private OffsetDateTime lastLoginAt;
    private OffsetDateTime lastLogoutAt;
    @Column(nullable = false)
    private boolean rememberMeEnabled;
    @Column(nullable = false)
    private OffsetDateTime createdAt;
    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    protected User() {}

    private User(String email, String passwordHash, String nickname) {
        this.publicId = UUID.randomUUID();
        this.email = email.trim().toLowerCase();
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.authProvider = "LOCAL";
        this.role = "USER";
        this.status = "ACTIVE";
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = createdAt;
    }

    public static User local(String email, String passwordHash, String nickname) {
        return new User(email, passwordHash, nickname);
    }
    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public String getEmail() { return email; }
    public String getNickname() { return nickname; }
}
