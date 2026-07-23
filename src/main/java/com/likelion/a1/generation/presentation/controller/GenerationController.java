package com.likelion.a1.generation.presentation.controller;

import com.likelion.a1.generation.application.service.GenerationAiService;
import com.likelion.a1.generation.domain.model.GenerationJob;
import com.likelion.a1.generation.presentation.dto.GenerationJobDtos.FalJobRequest;
import com.likelion.a1.generation.presentation.dto.GenerationJobDtos.PromptRequest;
import com.likelion.a1.generation.presentation.dto.GenerationJobDtos.Response;
import com.likelion.a1.generation.presentation.dto.GenerationJobDtos.ReversePromptRequest;
import com.likelion.a1.generation.presentation.dto.GenerationJobDtos.VideoGenerationRequest;
import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import com.likelion.a1.global.response.ApiResponse;
import com.likelion.a1.user.infrastructure.security.JwtPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 안티그래비티 다중 뎁스 비교 엔진 통합 API 명세서(v2.2) 규격의 프롬프트 보정/역프롬프트/fal.ai 제출·폴링 엔드포인트. */
@RestController
@RequestMapping("/api/v1/generation")
public class GenerationController {
  private final GenerationAiService generationAiService;

  public GenerationController(GenerationAiService generationAiService) {
    this.generationAiService = generationAiService;
  }

  @PostMapping("/prompts")
  public ApiResponse<Response> regeneratePrompt(
      @AuthenticationPrincipal JwtPrincipal principal, @Valid @RequestBody PromptRequest request) {
    verifyOwnership(principal, request.userId());
    GenerationJob job =
        generationAiService.regeneratePrompt(
            request.userId(), request.chatId(), request.imageBase64(), request.mimeType(), request.instruction());
    return ApiResponse.success(Response.from(job));
  }

  @PostMapping("/reverse-prompts")
  public ApiResponse<Response> reversePrompt(
      @AuthenticationPrincipal JwtPrincipal principal, @Valid @RequestBody ReversePromptRequest request) {
    verifyOwnership(principal, request.userId());
    GenerationJob job =
        generationAiService.reversePrompt(
            request.userId(), request.chatId(), request.imageBase64(), request.mimeType(), request.instruction());
    return ApiResponse.success(Response.from(job));
  }

  @PostMapping("/fal-jobs")
  public ApiResponse<Response> submitFalJob(
      @AuthenticationPrincipal JwtPrincipal principal, @Valid @RequestBody FalJobRequest request) {
    verifyOwnership(principal, request.userId());
    GenerationJob job =
        generationAiService.submitFalJob(
            request.userId(),
            request.chatId(),
            request.jobType(),
            request.modelCode(),
            request.input(),
            request.sheetType(),
            request.sheetValue());
    return ApiResponse.success(Response.from(job));
  }

  @PostMapping("/videos")
  public ApiResponse<Response> generateVideo(
      @AuthenticationPrincipal JwtPrincipal principal, @Valid @RequestBody VideoGenerationRequest request) {
    verifyOwnership(principal, request.userId());
    GenerationJob job =
        generationAiService.generateVideo(
            request.userId(),
            request.chatId(),
            request.highQuality(),
            request.images(),
            request.prompt(),
            request.duration(),
            request.aspectRatio(),
            request.refinePrompt());
    return ApiResponse.success(Response.from(job));
  }

  @GetMapping("/fal-jobs/{jobId}/status")
  public ApiResponse<Response> getFalJobStatus(@PathVariable Long jobId) {
    GenerationJob job = generationAiService.getStatus(jobId);
    return ApiResponse.success(Response.from(job));
  }

  /**
   * 요청 바디의 userId가 인증된 토큰 주체와 일치하는지 검증한다. principal이 null인 경우(테스트 환경,
   * 내부 호출 등)에도 NPE 대신 항상 이 분기로 안전하게 떨어져 인가 실패로 처리된다.
   */
  private void verifyOwnership(JwtPrincipal principal, Long requestUserId) {
    if (principal == null || principal.userId() == null || !principal.userId().equals(requestUserId)) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT, List.of("요청한 userId가 인증된 사용자와 일치하지 않습니다."));
    }
  }
}
