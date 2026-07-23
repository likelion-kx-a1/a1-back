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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
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
  private static final Logger log = LoggerFactory.getLogger(GenerationController.class);

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
            request.userId(),
            request.chatId(),
            request.imageBase64(),
            request.mimeType(),
            request.instruction(),
            request.parentMessageId());
    return ApiResponse.success(Response.from(job));
  }

  @PostMapping("/reverse-prompts")
  public ApiResponse<Response> reversePrompt(
      @AuthenticationPrincipal JwtPrincipal principal, @Valid @RequestBody ReversePromptRequest request) {
    verifyOwnership(principal, request.userId());
    GenerationJob job =
        generationAiService.reversePrompt(
            request.userId(),
            request.chatId(),
            request.imageBase64(),
            request.mimeType(),
            request.instruction(),
            request.parentMessageId());
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
            request.sheetValue(),
            request.parentMessageId());
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
            request.refinePrompt(),
            request.parentMessageId());
    return ApiResponse.success(Response.from(job));
  }

  @GetMapping("/fal-jobs/{jobId}/status")
  public ApiResponse<Response> getFalJobStatus(@PathVariable Long jobId) {
    GenerationJob job = getStatusWithConflictRetry(jobId);
    return ApiResponse.success(Response.from(job));
  }

  /**
   * getStatus()는 @Version 낙관적 락 충돌 시 그 트랜잭션 안에서 복구를 시도하지 않고 그대로 던진다
   * (Hibernate가 충돌 시점에 트랜잭션을 이미 rollback-only로 표시해버려서, 같은 트랜잭션 안에서 잡아
   * 정상 반환해도 커밋 시점에 UnexpectedRollbackException으로 터지기 때문 — 실제로 동시 요청으로 재현
   * 후 확인함). 트랜잭션 경계 밖인 여기서 한 번 더 호출하면, 완전히 새 트랜잭션으로 그 사이 다른 폴러가
   * 커밋해 둔 최신 상태(대개 이미 종결 상태)를 깨끗하게 다시 읽어온다.
   */
  private GenerationJob getStatusWithConflictRetry(Long jobId) {
    try {
      return generationAiService.getStatus(jobId);
    } catch (OptimisticLockingFailureException exception) {
      log.info("Job {} 동시 완료 처리 충돌 감지 — 새 트랜잭션으로 한 번 더 조회합니다.", jobId);
      return generationAiService.getStatus(jobId);
    }
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
