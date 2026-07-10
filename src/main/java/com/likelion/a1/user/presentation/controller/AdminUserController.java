package com.likelion.a1.user.presentation.controller;

import com.likelion.a1.global.response.ApiResponse;
import com.likelion.a1.user.application.service.AdminUserService;
import com.likelion.a1.user.infrastructure.security.JwtPrincipal;
import com.likelion.a1.user.presentation.dto.AdminUserDtos.ChangeAccountStatusRequest;
import com.likelion.a1.user.presentation.dto.AdminUserDtos.PageResponse;
import com.likelion.a1.user.presentation.dto.AdminUserDtos.RejectSignupRequest;
import com.likelion.a1.user.presentation.dto.AdminUserDtos.UserDetailResponse;
import com.likelion.a1.user.presentation.dto.AdminUserDtos.UserSummaryResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
  private final AdminUserService adminUserService;

  public AdminUserController(AdminUserService adminUserService) {
    this.adminUserService = adminUserService;
  }

  @GetMapping("/signup-requests")
  public ApiResponse<PageResponse<UserSummaryResponse>> getSignupRequests(
      @PageableDefault(size = 20) Pageable pageable) {
    return ApiResponse.success(
        "ADMIN_SIGNUP_REQUESTS_FETCHED",
        "회원가입 요청 목록을 조회했습니다.",
        adminUserService.getSignupRequests(pageable));
  }

  @GetMapping
  public ApiResponse<PageResponse<UserSummaryResponse>> getUsers(
      @RequestParam(required = false) String approvalStatus,
      @RequestParam(required = false) String accountStatus,
      @RequestParam(required = false) String keyword,
      @PageableDefault(size = 20) Pageable pageable) {
    return ApiResponse.success(
        "ADMIN_USERS_FETCHED",
        "회원 목록을 조회했습니다.",
        adminUserService.getUsers(approvalStatus, accountStatus, keyword, pageable));
  }

  @GetMapping("/{userId}")
  public ApiResponse<UserDetailResponse> getUser(@PathVariable Long userId) {
    return ApiResponse.success(
        "ADMIN_USER_FETCHED", "회원 상세 정보를 조회했습니다.", adminUserService.getUser(userId));
  }

  @PatchMapping("/{userId}/approve")
  public ApiResponse<UserDetailResponse> approveSignup(
      @PathVariable Long userId, @AuthenticationPrincipal JwtPrincipal principal) {
    return ApiResponse.success(
        "SIGNUP_APPROVED",
        "회원가입 요청을 승인했습니다.",
        adminUserService.approveSignup(userId, principal.userId()));
  }

  @PatchMapping("/{userId}/reject")
  public ApiResponse<UserDetailResponse> rejectSignup(
      @PathVariable Long userId, @Valid @RequestBody RejectSignupRequest request) {
    return ApiResponse.success(
        "SIGNUP_REJECTED",
        "회원가입 요청을 거절했습니다.",
        adminUserService.rejectSignup(userId, request.reason()));
  }

  @PatchMapping("/{userId}/status")
  public ApiResponse<UserDetailResponse> changeAccountStatus(
      @PathVariable Long userId,
      @AuthenticationPrincipal JwtPrincipal principal,
      @Valid @RequestBody ChangeAccountStatusRequest request) {
    return ApiResponse.success(
        "USER_ACCOUNT_STATUS_CHANGED",
        "회원 계정 상태를 변경했습니다.",
        adminUserService.changeAccountStatus(userId, principal.userId(), request.accountStatus()));
  }

  @DeleteMapping("/{userId}")
  public ApiResponse<Void> deleteUser(
      @PathVariable Long userId, @AuthenticationPrincipal JwtPrincipal principal) {
    adminUserService.deleteUser(userId, principal.userId());
    return ApiResponse.success("USER_DELETED", "회원을 삭제했습니다.", null);
  }
}
