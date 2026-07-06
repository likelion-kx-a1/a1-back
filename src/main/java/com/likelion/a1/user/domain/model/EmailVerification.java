package com.likelion.a1.user.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "email_verifications")
public class EmailVerification {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String email;

  @Column(nullable = false)
  private String verificationCodeHash;

  @Column(nullable = false, length = 30)
  private String purpose;

  @Column(nullable = false)
  private boolean verified;

  @Column(nullable = false)
  private OffsetDateTime expiredAt;

  private OffsetDateTime verifiedAt;

  @Column(nullable = false)
  private OffsetDateTime createdAt;
}
