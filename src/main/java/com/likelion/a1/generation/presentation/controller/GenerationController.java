package com.likelion.a1.generation.presentation.controller;

import com.likelion.a1.generation.application.service.GenerationAiService;
import com.likelion.a1.generation.domain.model.GenerationJob;
import com.likelion.a1.generation.presentation.dto.GenerationJobDtos.FalJobRequest;
import com.likelion.a1.generation.presentation.dto.GenerationJobDtos.PromptRequest;
import com.likelion.a1.generation.presentation.dto.GenerationJobDtos.Response;
import com.likelion.a1.generation.presentation.dto.GenerationJobDtos.ReversePromptRequest;
import com.likelion.a1.generation.presentation.dto.GenerationJobDtos.VideoGenerationRequest;
import com.likelion.a1.global.response.ApiResponse;
import jakarta.validation.Valid;
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
  public ApiResponse<Response> regeneratePrompt(@Valid @RequestBody PromptRequest request) {
    GenerationJob job =
        generationAiService.regeneratePrompt(
            request.userId(), request.chatId(), request.imageBase64(), request.mimeType(), request.instruction());
    return ApiResponse.success(Response.from(job));
  }

  @PostMapping("/reverse-prompts")
  public ApiResponse<Response> reversePrompt(@Valid @RequestBody ReversePromptRequest request) {
    GenerationJob job =
        generationAiService.reversePrompt(
            request.userId(), request.chatId(), request.imageBase64(), request.mimeType(), request.instruction());
    return ApiResponse.success(Response.from(job));
  }

  @PostMapping("/fal-jobs")
  public ApiResponse<Response> submitFalJob(@Valid @RequestBody FalJobRequest request) {
    GenerationJob job =
        generationAiService.submitFalJob(
            request.userId(), request.chatId(), request.jobType(), request.modelCode(), request.input());
    return ApiResponse.success(Response.from(job));
  }

  @PostMapping("/videos")
  public ApiResponse<Response> generateVideo(@Valid @RequestBody VideoGenerationRequest request) {
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
}
