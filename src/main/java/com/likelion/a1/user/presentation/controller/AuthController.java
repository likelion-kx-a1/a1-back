package com.likelion.a1.user.presentation.controller;

import com.likelion.a1.global.response.ApiResponse;
import com.likelion.a1.user.application.service.AuthService;
import com.likelion.a1.user.application.service.EmailVerificationService;
import com.likelion.a1.user.application.service.PasswordResetService;
import com.likelion.a1.user.infrastructure.security.JwtPrincipal;
import com.likelion.a1.user.presentation.dto.AuthDtos.EmailSendRequest;
import com.likelion.a1.user.presentation.dto.AuthDtos.EmailSendResponse;
import com.likelion.a1.user.presentation.dto.AuthDtos.EmailVerifyRequest;
import com.likelion.a1.user.presentation.dto.AuthDtos.EmailVerifyResponse;
import com.likelion.a1.user.presentation.dto.AuthDtos.LoginIdCheckResponse;
import com.likelion.a1.user.presentation.dto.AuthDtos.LoginRequest;
import com.likelion.a1.user.presentation.dto.AuthDtos.LoginResponse;
import com.likelion.a1.user.presentation.dto.AuthDtos.LogoutRequest;
import com.likelion.a1.user.presentation.dto.AuthDtos.PasswordResetRequest;
import com.likelion.a1.user.presentation.dto.AuthDtos.SignupRequest;
import com.likelion.a1.user.presentation.dto.AuthDtos.SignupResponse;
import com.likelion.a1.user.presentation.dto.AuthDtos.TokenRefreshRequest;
import com.likelion.a1.user.presentation.dto.AuthDtos.TokenRefreshResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService authService;
  private final EmailVerificationService emailVerificationService;
  private final PasswordResetService passwordResetService;

  public AuthController(
      AuthService authService,
      EmailVerificationService emailVerificationService,
      PasswordResetService passwordResetService) {
    this.authService = authService;
    this.emailVerificationService = emailVerificationService;
    this.passwordResetService = passwordResetService;
  }

  @GetMapping("/check-login-id")
  public ApiResponse<LoginIdCheckResponse> checkLoginId(@RequestParam String loginId) {
    return ApiResponse.success(
        "LOGIN_ID_AVAILABLE", "사용 가능한 아이디입니다.", authService.checkLoginId(loginId));
  }

  @PostMapping("/email/send")
  public ApiResponse<EmailSendResponse> sendEmail(@Valid @RequestBody EmailSendRequest request) {
    return ApiResponse.success(
        "EMAIL_SENT", "인증번호가 발송되었습니다.", emailVerificationService.send(request));
  }

  @PostMapping("/email/verify")
  public ApiResponse<EmailVerifyResponse> verifyEmail(
      @Valid @RequestBody EmailVerifyRequest request) {
    return ApiResponse.success(
        "EMAIL_VERIFIED", "이메일 인증이 완료되었습니다.", emailVerificationService.verify(request));
  }

  @PostMapping("/signup")
  public ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
    return ApiResponse.success(
        "SIGNUP_REQUESTED",
        "회원가입 신청이 완료되었습니다. 관리자 승인 후 이용 가능합니다.",
        authService.signup(request));
  }

  @PostMapping("/login")
  public ApiResponse<LoginResponse> login(
      @Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
    return ApiResponse.success(
        "LOGIN_SUCCESS",
        "로그인되었습니다.",
        authService.login(
            request, servletRequest.getRemoteAddr(), servletRequest.getHeader("User-Agent")));
  }

  @PostMapping("/refresh")
  public ApiResponse<TokenRefreshResponse> refresh(
      @Valid @RequestBody TokenRefreshRequest request) {
    return ApiResponse.success(
        "TOKEN_REFRESHED", "토큰이 재발급되었습니다.", authService.refresh(request));
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout(
      @AuthenticationPrincipal JwtPrincipal principal, @Valid @RequestBody LogoutRequest request) {
    authService.logout(principal.userId(), principal.sessionId(), request.refreshToken());
    return ApiResponse.success("LOGOUT_SUCCESS", "로그아웃되었습니다.", null);
  }

  @PostMapping("/password/reset")
  public ApiResponse<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
    passwordResetService.reset(request);
    return ApiResponse.success("PASSWORD_RESET_SUCCESS", "비밀번호가 재설정되었습니다.", null);
  }
}
