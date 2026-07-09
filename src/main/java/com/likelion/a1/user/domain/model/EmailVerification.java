package com.likelion.a1.user.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

  private OffsetDateTime usedAt;

  @Column(nullable = false)
  private OffsetDateTime createdAt;

  public static EmailVerification create(
      String email, String verificationCodeHash, String purpose, OffsetDateTime expiredAt) {
    EmailVerification verification = new EmailVerification();
    OffsetDateTime now = OffsetDateTime.now();

    verification.email = email.trim().toLowerCase();
    verification.verificationCodeHash = verificationCodeHash;
    verification.purpose = purpose;
    verification.verified = false;
    verification.expiredAt = expiredAt;
    verification.createdAt = now;

    return verification;
  }

  public void verify() {
    this.verified = true;
    this.verifiedAt = OffsetDateTime.now();
  }

  public void markUsed() {
    this.usedAt = OffsetDateTime.now();
  }

  public boolean isExpired() {
    return expiredAt.isBefore(OffsetDateTime.now());
  }

  public boolean isUsed() {
    return usedAt != null;
  }
}
