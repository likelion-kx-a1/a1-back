package com.likelion.a1.user.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String passwordHash;

  @Column(nullable = false, length = 100)
  private String nickname;

  @Column(columnDefinition = "text")
  private String profileImageUrl;

  @Column(nullable = false, length = 30)
  private String role;

  @Column(nullable = false, length = 30)
  private String status;

  @Column(nullable = false)
  private int loginCount;

  private OffsetDateTime approvedAt;

  private OffsetDateTime lastLoginAt;

  private OffsetDateTime lastLogoutAt;

  @Column(nullable = false)
  private OffsetDateTime createdAt;

  private OffsetDateTime deletedAt;

  private User(String email, String passwordHash, String nickname) {
    this.email = email.trim().toLowerCase();
    this.passwordHash = passwordHash;
    this.nickname = nickname;
    this.role = "USER";
    this.status = "PENDING";
    this.createdAt = OffsetDateTime.now();
  }

  public static User local(String email, String passwordHash, String nickname) {
    return new User(email, passwordHash, nickname);
  }
}
