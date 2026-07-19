package com.likelion.a1.user.domain.model;

import jakarta.persistence.*;
import java.time.LocalDate;
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

  @Column(nullable = false, unique = true, length = 100)
  private String loginId;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String passwordHash;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false)
  private LocalDate birthDate;

  @Column(nullable = false, length = 30)
  private String phoneNumber;

  @Column(columnDefinition = "text")
  private String profileImageUrl;

  @Column(nullable = false, length = 30)
  private String role = "USER";

  @Column(nullable = false, length = 30)
  private String accountStatus = "INACTIVE";

  @Column(nullable = false, length = 30)
  private String approvalStatus = "PENDING";

  private Long approvedBy;
  private OffsetDateTime approvedAt;
  private OffsetDateTime rejectedAt;

  @Column(columnDefinition = "text")
  private String rejectionReason;

  @Column(nullable = false)
  private int loginCount;

  private OffsetDateTime lastLoginAt;
  private OffsetDateTime lastLogoutAt;

  @Column(nullable = false)
  private OffsetDateTime createdAt;

  @Column(nullable = false)
  private OffsetDateTime updatedAt;

  private OffsetDateTime deletedAt;

  private User(String email, String passwordHash, String name) {
    this.loginId = email.trim().toLowerCase();
    this.email = email.trim().toLowerCase();
    this.passwordHash = passwordHash;
    this.name = name;
    this.createdAt = OffsetDateTime.now();
    this.updatedAt = this.createdAt;
  }

  public static User local(String email, String passwordHash, String name) {
    return new User(email, passwordHash, name);
  }
  public static User signup(
    String loginId, 
    String email, 
    String passwordHash, 
    String name, 
    LocalDate birthDate, 
    String phoneNumber
  ) {
    User user = new User();
    OffsetDateTime now = OffsetDateTime.now();

    user.loginId = loginId;
    user.email = email.trim().toLowerCase();
    user.passwordHash = passwordHash;
    user.name = name.trim();
    user.birthDate = birthDate;
    user.phoneNumber = phoneNumber.trim();
    user.role = "USER";
    user.accountStatus = "INACTIVE";
    user.approvalStatus = "PENDING";
    user.loginCount = 0;
    user.createdAt = now;
    user.updatedAt = now;

    return user;
  }

  public void recordLogin(){
    this.loginCount++;
    this.lastLoginAt = OffsetDateTime.now();
    this.updatedAt = this.lastLoginAt;
  }

  public void recordLogout(){
    this.lastLogoutAt = OffsetDateTime.now();
    this.updatedAt = this.lastLogoutAt;
  }

  public void changePassword(String newPasswordHash) {
    this.passwordHash = newPasswordHash;
    this.updatedAt = OffsetDateTime.now();
  }

  public void approve(Long adminUserId) {
    OffsetDateTime now = OffsetDateTime.now();

    this.approvalStatus = "APPROVED";
    this.accountStatus = "ACTIVE";
    this.approvedBy = adminUserId;
    this.approvedAt = now;
    this.rejectedAt = null;
    this.rejectionReason = null;
    this.updatedAt = now;
  }

  public void reject(String reason) {
    OffsetDateTime now = OffsetDateTime.now();

    this.approvalStatus = "REJECTED";
    this.accountStatus = "INACTIVE";
    this.rejectedAt = now;
    this.rejectionReason = reason;
    this.updatedAt = now;
  }

  public void changeAccountStatus(String accountStatus) {
    this.accountStatus = accountStatus;
    this.updatedAt = OffsetDateTime.now();
  }

  public void delete() {
    OffsetDateTime now = OffsetDateTime.now();

    this.accountStatus = "INACTIVE";
    this.deletedAt = now;
    this.updatedAt = now;
  }

  public boolean isDeleted() {
    return this.deletedAt != null;
  }

  public boolean isLoginAllowed() {
    return deletedAt==null
        && "APPROVED".equals(this.approvalStatus)
        && "ACTIVE".equals(accountStatus);
  }
}
