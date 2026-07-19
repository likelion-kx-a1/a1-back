package com.likelion.a1.user.application.service;

import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import com.likelion.a1.user.application.port.out.EmailSenderPort;
import com.likelion.a1.user.domain.model.User;
import com.likelion.a1.user.domain.repository.UserRepository;
import com.likelion.a1.user.presentation.dto.AdminUserDtos.PageResponse;
import com.likelion.a1.user.presentation.dto.AdminUserDtos.UserDetailResponse;
import com.likelion.a1.user.presentation.dto.AdminUserDtos.UserSummaryResponse;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class AdminUserService {
  private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);
  private static final Set<String> APPROVAL_STATUSES = Set.of("PENDING", "APPROVED", "REJECTED");
  private static final Set<String> ACCOUNT_STATUSES = Set.of("ACTIVE", "INACTIVE");

  private final UserRepository userRepository;
  private final EmailSenderPort emailSenderPort;

  public AdminUserService(UserRepository userRepository, EmailSenderPort emailSenderPort) {
    this.userRepository = userRepository;
    this.emailSenderPort = emailSenderPort;
  }

  @Transactional(readOnly = true)
  public PageResponse<UserSummaryResponse> getSignupRequests(Pageable pageable) {
    return toPageResponse(userRepository.findSignupRequests(pageable).map(this::toSummary));
  }

  @Transactional(readOnly = true)
  public PageResponse<UserSummaryResponse> getUsers(
      String approvalStatus, String accountStatus, String keyword, Pageable pageable) {
    String normalizedApprovalStatus = normalizeStatus(approvalStatus);
    String normalizedAccountStatus = normalizeStatus(accountStatus);

    validateApprovalStatusIfPresent(normalizedApprovalStatus);
    validateAccountStatusIfPresent(normalizedAccountStatus);

    return toPageResponse(
        userRepository
            .searchUsers(
                normalizedApprovalStatus, normalizedAccountStatus, normalizeKeyword(keyword), pageable)
            .map(this::toSummary));
  }

  @Transactional(readOnly = true)
  public UserDetailResponse getUser(Long userId) {
    return toDetail(findActiveUser(userId));
  }

  public UserDetailResponse approveSignup(Long userId, Long adminUserId) {
    User user = findActiveUser(userId);

    if ("APPROVED".equals(user.getApprovalStatus())) {
      throw new BusinessException(ErrorCode.SIGNUP_ALREADY_PROCESSED);
    }

    user.approve(adminUserId);
    User savedUser = userRepository.save(user);
    sendSignupApprovedEmailAfterCommit(savedUser);

    return toDetail(savedUser);
  }

  public UserDetailResponse rejectSignup(Long userId, String reason) {
    User user = findActiveUser(userId);

    if ("APPROVED".equals(user.getApprovalStatus())) {
      throw new BusinessException(ErrorCode.SIGNUP_ALREADY_PROCESSED);
    }

    user.reject(reason.trim());
    return toDetail(userRepository.save(user));
  }

  public UserDetailResponse changeAccountStatus(
      Long userId, Long adminUserId, String accountStatus) {
    preventSelfMutation(userId, adminUserId);

    String normalizedAccountStatus = normalizeStatus(accountStatus);
    validateAccountStatusIfPresent(normalizedAccountStatus);

    User user = findActiveUser(userId);
    user.changeAccountStatus(normalizedAccountStatus);

    return toDetail(userRepository.save(user));
  }

  public void deleteUser(Long userId, Long adminUserId) {
    preventSelfMutation(userId, adminUserId);

    User user = findActiveUser(userId);
    user.delete();
    userRepository.save(user);
  }

  private User findActiveUser(Long userId) {
    User user =
        userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    if (user.isDeleted()) {
      throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }

    return user;
  }

  private void preventSelfMutation(Long userId, Long adminUserId) {
    if (userId.equals(adminUserId)) {
      throw new BusinessException(ErrorCode.ADMIN_CANNOT_UPDATE_SELF);
    }
  }

  private void sendSignupApprovedEmailAfterCommit(User user) {
    Runnable sendMail =
        () -> {
          try {
            emailSenderPort.sendSignupApproved(user.getEmail(), user.getName());
          } catch (RuntimeException exception) {
            log.warn("Failed to send signup approved email. userId={}", user.getId(), exception);
          }
        };

    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      sendMail.run();
      return;
    }

    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            sendMail.run();
          }
        });
  }

  private void validateApprovalStatusIfPresent(String approvalStatus) {
    if (approvalStatus != null && !APPROVAL_STATUSES.contains(approvalStatus)) {
      throw new BusinessException(ErrorCode.INVALID_APPROVAL_STATUS);
    }
  }

  private void validateAccountStatusIfPresent(String accountStatus) {
    if (accountStatus != null && !ACCOUNT_STATUSES.contains(accountStatus)) {
      throw new BusinessException(ErrorCode.INVALID_ACCOUNT_STATUS);
    }
  }

  private String normalizeStatus(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }

    return value.trim().toUpperCase();
  }

  private String normalizeKeyword(String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return null;
    }

    return keyword.trim();
  }

  private UserSummaryResponse toSummary(User user) {
    return new UserSummaryResponse(
        user.getId(),
        user.getLoginId(),
        user.getEmail(),
        user.getName(),
        user.getRole(),
        user.getAccountStatus(),
        user.getApprovalStatus(),
        user.getLoginCount(),
        user.getLastLoginAt(),
        user.getCreatedAt());
  }

  private UserDetailResponse toDetail(User user) {
    return new UserDetailResponse(
        user.getId(),
        user.getLoginId(),
        user.getEmail(),
        user.getName(),
        user.getBirthDate(),
        user.getPhoneNumber(),
        user.getProfileImageUrl(),
        user.getRole(),
        user.getAccountStatus(),
        user.getApprovalStatus(),
        user.getApprovedBy(),
        user.getApprovedAt(),
        user.getRejectedAt(),
        user.getRejectionReason(),
        user.getLoginCount(),
        user.getLastLoginAt(),
        user.getLastLogoutAt(),
        user.getCreatedAt(),
        user.getUpdatedAt(),
        user.getDeletedAt());
  }

  private <T> PageResponse<T> toPageResponse(Page<T> page) {
    return new PageResponse<>(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}
