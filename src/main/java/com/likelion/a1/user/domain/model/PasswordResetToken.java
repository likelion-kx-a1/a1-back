package com.likelion.a1.user.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long userId;

  @Column(nullable = false)
  private String resetTokenHash;

  @Column(nullable = false)
  private OffsetDateTime expiredAt;

  private OffsetDateTime usedAt;

  @Column(nullable = false)
  private OffsetDateTime createdAt;
}
