package com.likelion.a1.generation.application.service;

import com.likelion.a1.generation.application.port.out.AiTextGenerationResult;
import com.likelion.a1.generation.application.port.out.CharacterSheetPromptPort;
import com.likelion.a1.generation.application.port.out.FalGenerationPort;
import com.likelion.a1.generation.application.port.out.FalGenerationStatus;
import com.likelion.a1.generation.application.port.out.FalGenerationSubmission;
import com.likelion.a1.generation.application.port.out.ImageAnalysisPort;
import com.likelion.a1.generation.application.port.out.PromptGenerationPort;
import com.likelion.a1.generation.domain.model.CharacterSheetSettings;
import com.likelion.a1.generation.domain.model.GenerationJob;
import com.likelion.a1.generation.domain.model.GenerationMode;
import com.likelion.a1.generation.domain.model.GenerationStatus;
import com.likelion.a1.generation.domain.model.GenerationType;
import com.likelion.a1.generation.domain.repository.GenerationJobRepository;
import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;

/** 프롬프트 보정, 역프롬프트 추출, fal.ai 비동기 제출/폴링 전체 파이프라인을 조율하고 GenerationJob으로 영속화한다. */
@Service
@Transactional
public class GenerationAiService {
  private static final Logger log = LoggerFactory.getLogger(GenerationAiService.class);
  private static final int MAX_REFERENCE_IMAGES = 9;

  private final GenerationJobRepository generationJobRepository;
  private final PromptGenerationPort promptGenerationPort;
  private final ImageAnalysisPort imageAnalysisPort;
  private final FalGenerationPort falGenerationPort;
  private final CharacterSheetPromptPort characterSheetPromptPort;
  private final GeneratedMediaUploader generatedMediaUploader;
  private final GenerationResultService generationResultService;

  public GenerationAiService(
      GenerationJobRepository generationJobRepository,
      PromptGenerationPort promptGenerationPort,
      ImageAnalysisPort imageAnalysisPort,
      FalGenerationPort falGenerationPort,
      CharacterSheetPromptPort characterSheetPromptPort,
      GeneratedMediaUploader generatedMediaUploader,
      GenerationResultService generationResultService) {
    this.generationJobRepository = generationJobRepository;
    this.promptGenerationPort = promptGenerationPort;
    this.imageAnalysisPort = imageAnalysisPort;
    this.falGenerationPort = falGenerationPort;
    this.characterSheetPromptPort = characterSheetPromptPort;
    this.generatedMediaUploader = generatedMediaUploader;
    this.generationResultService = generationResultService;
  }

  public GenerationJob regeneratePrompt(
      Long userId,
      Long chatId,
      String imageBase64,
      String mimeType,
      String instruction,
      Long parentMessageId) {
    byte[] imageBytes = imageBase64 == null || imageBase64.isBlank() ? null : decodeImage(imageBase64);

    Map<String, Object> requestPayload = new LinkedHashMap<>();
    requestPayload.put("mimeType", mimeType);

    GenerationJob job =
        generationJobRepository.save(
            GenerationJob.create(
                userId,
                chatId,
                null,
                parentMessageId,
                GenerationType.PROMPT_REGENERATION.name(),
                instruction,
                requestPayload));
    generationResultService.startGenerating(userId, chatId);

    try {
      long startedAtMs = System.currentTimeMillis();
      AiTextGenerationResult result = promptGenerationPort.generateFromImage(imageBytes, mimeType, instruction);
      long refineDurationMs = System.currentTimeMillis() - startedAtMs;

      Map<String, Object> responsePayload = toResponsePayload(result);
      PerformanceMetrics.record(responsePayload, "refineDurationMs", refineDurationMs);
      PerformanceMetrics.announce(job.getId(), responsePayload);
      job.complete(responsePayload);
      generationResultService.saveAssistantTextResult(
          userId, chatId, job.getRequestMessageId(), job.getId(), result.text(), null, null);
    } catch (RestClientResponseException exception) {
      job.fail(exception.getMessage());
      generationJobRepository.save(job);
      safeFinishGenerating(userId, chatId);
      throw exception;
    }

    return generationJobRepository.save(job);
  }

  public GenerationJob reversePrompt(
      Long userId,
      Long chatId,
      String imageBase64,
      String mimeType,
      String instruction,
      Long parentMessageId) {
    byte[] imageBytes = decodeImage(imageBase64);

    Map<String, Object> requestPayload = new LinkedHashMap<>();
    requestPayload.put("mimeType", mimeType);

    GenerationJob job =
        generationJobRepository.save(
            GenerationJob.create(
                userId,
                chatId,
                null,
                parentMessageId,
                GenerationType.REVERSE_PROMPT.name(),
                instruction,
                requestPayload));
    generationResultService.startGenerating(userId, chatId);

    try {
      long startedAtMs = System.currentTimeMillis();
      AiTextGenerationResult result = imageAnalysisPort.analyze(imageBytes, mimeType, instruction);
      long analysisDurationMs = System.currentTimeMillis() - startedAtMs;

      Map<String, Object> responsePayload = toResponsePayload(result);
      PerformanceMetrics.record(responsePayload, "analysisDurationMs", analysisDurationMs);
      PerformanceMetrics.announce(job.getId(), responsePayload);
      job.complete(responsePayload);
      generationResultService.saveAssistantTextResult(
          userId, chatId, job.getRequestMessageId(), job.getId(), result.text(), null, null);
    } catch (RestClientResponseException exception) {
      job.fail(exception.getMessage());
      generationJobRepository.save(job);
      safeFinishGenerating(userId, chatId);
      throw exception;
    }

    return generationJobRepository.save(job);
  }

  public GenerationJob submitFalJob(
      Long userId,
      Long chatId,
      String jobType,
      String modelCode,
      Map<String, Object> input,
      String sheetType,
      String sheetValue,
      String generationMode,
      Boolean aiEnhance,
      CharacterSheetSettings characterSettings,
      Long parentMessageId) {
    GenerationType type = parseGenerationType(jobType);

    // 캐릭터 시트(Character Design Reference Sheet) 생성 모드는 시트 주입 엔진과 무관한 전용
    // 파이프라인이라 sheetType/sheetValue는 무시하고 별도 분기로 처리한다(charactersheet.md 규격).
    if (GenerationMode.fromRequest(generationMode) == GenerationMode.CHARACTER_SHEET) {
      return submitCharacterSheetFalJob(
          userId, chatId, type, modelCode, input, aiEnhance, characterSettings, parentMessageId);
    }

    String originalPrompt = input.get("prompt") instanceof String promptText ? promptText : null;

    // 시트(Sheet) 주입 엔진: 시트 유형/생성 모드(이미지·비디오)에 따라 fal.ai로 보낼 최종 prompt를
    // 조건부로 결합한다(api_3.md 규격). sheetType이 없거나 NONE이면 원본 프롬프트가 그대로 반환된다.
    String composedPrompt = SheetPromptComposer.compose(originalPrompt, type, sheetType, sheetValue);
    String finalPrompt = composedPrompt.isBlank() ? null : composedPrompt;

    Map<String, Object> effectiveInput = new LinkedHashMap<>(input);
    if (finalPrompt != null) {
      effectiveInput.put("prompt", finalPrompt);
    }
    String effectiveModelCode = resolveImageModelCode(type, modelCode, effectiveInput);

    Map<String, Object> requestPayload = new LinkedHashMap<>();
    requestPayload.put("modelCode", effectiveModelCode);
    requestPayload.put("input", effectiveInput);
    requestPayload.put("sheetType", sheetType);
    requestPayload.put("sheetValue", sheetValue);
    if (!Objects.equals(originalPrompt, finalPrompt)) {
      requestPayload.put("originalPrompt", originalPrompt);
    }

    GenerationJob job =
        generationJobRepository.save(
            GenerationJob.create(
                userId, chatId, null, parentMessageId, type.name(), finalPrompt, requestPayload));
    generationResultService.startGenerating(userId, chatId);

    try {
      long startedAtMs = System.currentTimeMillis();
      FalGenerationSubmission submission = falGenerationPort.submit(effectiveModelCode, effectiveInput);
      long submissionLatencyMs = System.currentTimeMillis() - startedAtMs;

      Map<String, Object> responsePayload = new LinkedHashMap<>();
      responsePayload.put("requestId", submission.externalRequestId());
      responsePayload.put("statusUrl", submission.statusUrl());
      responsePayload.put("responseUrl", submission.responseUrl());
      responsePayload.put("raw", submission.rawResponse());
      PerformanceMetrics.record(responsePayload, "submissionLatencyMs", submissionLatencyMs);
      PerformanceMetrics.announce(job.getId(), responsePayload);
      job.markQueued(responsePayload);
    } catch (RestClientResponseException exception) {
      job.fail(exception.getMessage());
      generationJobRepository.save(job);
      safeFinishGenerating(userId, chatId);
      throw exception;
    }

    // 실제 완료 처리(채팅 메시지/에셋 저장, isGenerating 해제)는 비동기 완료 시점
    // (getStatus 수동 폴링 또는 GenerationVideoPollingScheduler)에 GeneratedMediaUploader가 수행한다.
    return generationJobRepository.save(job);
  }

  /**
   * 캐릭터 시트(Character Design Reference Sheet) 생성 모드 전용 분기(charactersheet.md 규격).
   * aiEnhance=true면 Claude Sonnet({@link CharacterSheetPromptPort})이 고정 레이아웃 폼으로 재구성한
   * final_prompt_body를, false(또는 미지정)면 로컬 템플릿({@link CharacterSheetPromptTemplate#buildDirect})을
   * 사용하고, 두 경우 모두 고정 Requirements 블록({@link CharacterSheetPromptTemplate#appendRequirements})을
   * 덧붙인 뒤 fal.ai(GPT Image 2)로 제출한다. job.prompt에는 참조가 남도록 사용자의 core_description
   * (원본)을 저장하고, 실제 결합된 최종 프롬프트는 responsePayload.finalPrompt에 기록한다
   * (generateVideo의 refinedPrompt 기록 방식과 동일한 관례).
   */
  private GenerationJob submitCharacterSheetFalJob(
      Long userId,
      Long chatId,
      GenerationType type,
      String modelCode,
      Map<String, Object> input,
      Boolean aiEnhance,
      CharacterSheetSettings characterSettings,
      Long parentMessageId) {
    String coreDescription = input.get("prompt") instanceof String promptText ? promptText : null;
    boolean hasReferenceImages = input.get("images") instanceof List<?> images && !images.isEmpty();
    CharacterSheetSettings settings = CharacterSheetSettings.orEmpty(characterSettings);
    boolean shouldEnhance = Boolean.TRUE.equals(aiEnhance);
    String effectiveModelCode = resolveImageModelCode(type, modelCode, input);

    Map<String, Object> requestPayload = new LinkedHashMap<>();
    requestPayload.put("modelCode", effectiveModelCode);
    requestPayload.put("input", input);
    requestPayload.put("generationMode", "character_sheet");
    requestPayload.put("aiEnhance", shouldEnhance);
    requestPayload.put("characterSettings", settings);

    GenerationJob job =
        generationJobRepository.save(
            GenerationJob.create(
                userId, chatId, null, parentMessageId, type.name(), coreDescription, requestPayload));
    generationResultService.startGenerating(userId, chatId);

    try {
      long composeStartedAtMs = System.currentTimeMillis();
      String promptBody =
          shouldEnhance
              ? characterSheetPromptPort
                  .generateFinalPromptBody(coreDescription, settings, hasReferenceImages)
                  .text()
              : CharacterSheetPromptTemplate.buildDirect(coreDescription, settings, hasReferenceImages);
      String finalPrompt = CharacterSheetPromptTemplate.appendRequirements(promptBody);
      long composeDurationMs = System.currentTimeMillis() - composeStartedAtMs;

      Map<String, Object> effectiveInput = new LinkedHashMap<>(input);
      effectiveInput.put("prompt", finalPrompt);

      long submitStartedAtMs = System.currentTimeMillis();
      FalGenerationSubmission submission = falGenerationPort.submit(effectiveModelCode, effectiveInput);
      long submissionLatencyMs = System.currentTimeMillis() - submitStartedAtMs;

      Map<String, Object> responsePayload = new LinkedHashMap<>();
      responsePayload.put("finalPrompt", finalPrompt);
      responsePayload.put("requestId", submission.externalRequestId());
      responsePayload.put("statusUrl", submission.statusUrl());
      responsePayload.put("responseUrl", submission.responseUrl());
      responsePayload.put("raw", submission.rawResponse());
      PerformanceMetrics.record(responsePayload, "characterSheetPromptDurationMs", composeDurationMs);
      PerformanceMetrics.record(responsePayload, "submissionLatencyMs", submissionLatencyMs);
      PerformanceMetrics.announce(job.getId(), responsePayload);
      job.markQueued(responsePayload);
    } catch (RestClientResponseException exception) {
      job.fail(exception.getMessage());
      generationJobRepository.save(job);
      safeFinishGenerating(userId, chatId);
      throw exception;
    }

    return generationJobRepository.save(job);
  }

  private String resolveImageModelCode(
      GenerationType type, String modelCode, Map<String, Object> input) {
    boolean isImageGeneration =
        type == GenerationType.IMAGE_GENERATION || type == GenerationType.IMAGE_VARIATION;
    boolean hasReferenceImages =
        input.get("images") instanceof List<?> images && !images.isEmpty();

    if (isImageGeneration
        && hasReferenceImages
        && "openai/gpt-image-2".equals(modelCode)) {
      return "openai/gpt-image-2/edit";
    }
    return modelCode;
  }

  /**
   * 이미지 개수(0/1/2장 이상)와 highQuality 플래그로 fal.ai 모델을 자동 분기 선택하고, refinePrompt가
   * true(또는 미지정)이면 Claude Sonnet 물리 보정을 선행 실행해 그 결과를 최종 prompt로 주입한 뒤 비동기
   * 큐에 제출한다(api_2.md 분기 엔진 규격). refinePrompt=false면 사용자가 입력한 원본 prompt를 그대로 쓴다.
   */
  public GenerationJob generateVideo(
      Long userId,
      Long chatId,
      boolean highQuality,
      List<String> images,
      String prompt,
      Integer duration,
      String aspectRatio,
      Boolean refinePrompt,
      Long parentMessageId) {
    List<String> safeImages = images == null ? List.of() : images;
    if (safeImages.size() > MAX_REFERENCE_IMAGES) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT, List.of("참조 이미지는 최대 " + MAX_REFERENCE_IMAGES + "장까지만 지원합니다."));
    }
    boolean shouldRefine = refinePrompt == null || refinePrompt;

    String modelCode = resolveVideoModelCode(highQuality, safeImages.size());

    Map<String, Object> requestPayload = new LinkedHashMap<>();
    requestPayload.put("modelCode", modelCode);
    requestPayload.put("highQuality", highQuality);
    requestPayload.put("images", safeImages);
    requestPayload.put("duration", duration);
    requestPayload.put("aspectRatio", aspectRatio);
    requestPayload.put("refinePromptEnabled", shouldRefine);

    GenerationJob job =
        generationJobRepository.save(
            GenerationJob.create(
                userId,
                chatId,
                null,
                parentMessageId,
                GenerationType.VIDEO_GENERATION.name(),
                prompt,
                requestPayload));
    generationResultService.startGenerating(userId, chatId);

    try {
      Map<String, Object> responsePayload = new LinkedHashMap<>();
      String finalPrompt = prompt;

      if (shouldRefine) {
        long refineStartedAtMs = System.currentTimeMillis();
        byte[] referenceImageBytes = safeImages.isEmpty() ? null : tryDecodeImage(safeImages.get(0));
        AiTextGenerationResult refined =
            promptGenerationPort.generateFromImage(referenceImageBytes, "image/png", prompt);
        long refineDurationMs = System.currentTimeMillis() - refineStartedAtMs;

        finalPrompt = refined.text();
        responsePayload.put("refinedPrompt", finalPrompt);
        PerformanceMetrics.record(responsePayload, "refineDurationMs", refineDurationMs);
      }

      Map<String, Object> input = new LinkedHashMap<>();
      input.put("prompt", finalPrompt);
      if (duration != null) {
        input.put("duration", duration);
      }
      if (aspectRatio != null) {
        input.put("aspect_ratio", aspectRatio);
      }
      if (!safeImages.isEmpty()) {
        input.put("images", safeImages);
      }

      long submitStartedAtMs = System.currentTimeMillis();
      FalGenerationSubmission submission = falGenerationPort.submit(modelCode, input);
      long submissionLatencyMs = System.currentTimeMillis() - submitStartedAtMs;

      responsePayload.put("requestId", submission.externalRequestId());
      responsePayload.put("statusUrl", submission.statusUrl());
      responsePayload.put("responseUrl", submission.responseUrl());
      responsePayload.put("raw", submission.rawResponse());
      PerformanceMetrics.record(responsePayload, "submissionLatencyMs", submissionLatencyMs);
      PerformanceMetrics.announce(job.getId(), responsePayload);
      job.markQueued(responsePayload);
    } catch (RestClientResponseException exception) {
      job.fail(exception.getMessage());
      generationJobRepository.save(job);
      safeFinishGenerating(userId, chatId);
      throw exception;
    }

    // 실제 완료 처리(채팅 메시지/에셋 저장, isGenerating 해제)는 비동기 완료 시점
    // (getStatus 수동 폴링 또는 GenerationVideoPollingScheduler)에 GeneratedMediaUploader가 수행한다.
    return generationJobRepository.save(job);
  }

  public GenerationJob getStatus(Long jobId, Long userId) {
    GenerationJob job = findJob(jobId);
    verifyOwnership(job, userId);
    if (isTerminal(job.getStatus())) {
      return job;
    }

    Map<String, Object> requestPayload = job.getRequestPayload();
    Map<String, Object> responsePayload = job.getResponsePayload();
    if (requestPayload == null || responsePayload == null) {
      return job;
    }

    Object modelCodeValue = requestPayload.get("modelCode");
    Object externalRequestIdValue = responsePayload.get("requestId");
    if (!(modelCodeValue instanceof String modelCode) || !(externalRequestIdValue instanceof String externalRequestId)) {
      return job;
    }

    String statusUrl = stringValue(responsePayload.get("statusUrl"));
    String responseUrl = stringValue(responsePayload.get("responseUrl"));
    FalGenerationStatus polled =
        falGenerationPort.poll(modelCode, externalRequestId, statusUrl, responseUrl);
    GenerationStatus newStatus = GenerationStatus.fromFalStatus(polled.status());

    Map<String, Object> merged = new LinkedHashMap<>(responsePayload);
    merged.putAll(polled.rawResponse());

    newStatus = generatedMediaUploader.applyCompletion(job, newStatus, merged);
    if (newStatus == null) {
      // 다른 폴러(백그라운드 스케줄러)가 이미 이 job을 종결 처리했다 — 저장을 시도하면 100% 낙관적
      // 락 충돌만 나므로 시도하지 않고, DB에 이미 반영된 최신(종결) 상태를 그대로 다시 읽어 반환한다.
      return findJob(jobId);
    }

    job.applyPolledStatus(newStatus, merged, stringValue(polled.rawResponse().get("errorMessage")));
    // 그래도 남는 좁은 race window(위 체크 이후 저장 사이)에서 OptimisticLockingFailureException이
    // 나면 여기서 잡지 않는다: Hibernate가 버전 충돌이 나는 순간 이 트랜잭션을 이미 rollback-only로
    // 표시하기 때문에, 이 메서드 안에서 잡아서 "복구"를 시도해도 메서드가 정상 반환된 뒤 커밋 시점에
    // UnexpectedRollbackException으로 터진다(실측 확인됨). 그대로 던져 트랜잭션이 깨끗하게 롤백되게
    // 하고, 복구(재조회)는 트랜잭션 경계 밖인 GenerationController에서 한 번 재시도하는 방식으로 처리한다.
    return generationJobRepository.save(job);
  }

  private String stringValue(Object value) {
    return value instanceof String string && !string.isBlank() ? string : null;
  }

  /**
   * job.fail() 이후 isGenerating 해제를 시도한다. 채팅방이 그 사이 삭제되는 등으로 이 호출이 실패해도,
   * 원래 발생한 AI 공급자 예외(catch 블록에서 뒤이어 rethrow됨)를 가려서는 안 되므로 로그만 남기고 넘어간다.
   */
  private void safeFinishGenerating(Long userId, Long chatId) {
    try {
      generationResultService.finishGenerating(userId, chatId);
    } catch (RuntimeException exception) {
      log.warn(
          "Chat {} (user {}) isGenerating 해제 실패 — 무시하고 원래 예외를 전파합니다.",
          chatId,
          userId,
          exception);
    }
  }

  private GenerationJob findJob(Long jobId) {
    return generationJobRepository
        .findById(jobId)
        .orElseThrow(() -> new BusinessException(ErrorCode.GENERATION_NOT_FOUND));
  }

  /**
   * job 상태 조회는 경로 변수(jobId)만으로 호출되므로, 요청 바디의 userId와 principal을 비교하는
   * {@code GenerationController.verifyOwnership}과 달리 여기서는 job에 실제로 저장된 소유자와
   * 인증된 principal을 비교한다(docs_h/보안_취약점_점검.md #2 — IDOR 방지).
   */
  private void verifyOwnership(GenerationJob job, Long userId) {
    if (!job.getUserId().equals(userId)) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT, List.of("요청한 사용자가 이 생성 작업의 소유자가 아닙니다."));
    }
  }

  private GenerationType parseGenerationType(String jobType) {
    try {
      return GenerationType.valueOf(jobType);
    } catch (IllegalArgumentException exception) {
      throw new BusinessException(ErrorCode.INVALID_INPUT);
    }
  }

  private Map<String, Object> toResponsePayload(AiTextGenerationResult result) {
    Map<String, Object> responsePayload = new LinkedHashMap<>();
    responsePayload.put("text", result.text());
    responsePayload.put("raw", result.rawResponse());
    return responsePayload;
  }

  private byte[] decodeImage(String imageBase64) {
    try {
      return Base64.getDecoder().decode(imageBase64);
    } catch (IllegalArgumentException exception) {
      throw new BusinessException(ErrorCode.INVALID_INPUT);
    }
  }

  /**
   * highQuality=true -> ByteDance Seedance 2.0, false -> fal.ai Kling O3 Standard.
   * 이미지 0장은 text-to-video, 1장은 image-to-video, 2장 이상은 reference-to-video로 분기한다.
   */
  private String resolveVideoModelCode(boolean highQuality, int imageCount) {
    String family = highQuality ? "bytedance/seedance-2.0" : "fal-ai/kling-video/o3/standard";
    String variant =
        imageCount == 0 ? "text-to-video" : imageCount == 1 ? "image-to-video" : "reference-to-video";
    return family + "/" + variant;
  }

  /**
   * Claude 보정 선행 호출에 참고 이미지로 쓸 바이트를 시도해 디코딩한다. images 원소는 Base64/데이터 URL일
   * 수도, S3 영구 URL일 수도 있다(api_2.md 2번 규격) — S3 URL은 다운로드하지 않고 텍스트만으로 보정한다.
   */
  private byte[] tryDecodeImage(String imageValue) {
    if (imageValue == null || imageValue.isBlank() || imageValue.startsWith("http://")
        || imageValue.startsWith("https://")) {
      return null;
    }
    try {
      String base64Part = imageValue.contains(",") ? imageValue.substring(imageValue.indexOf(',') + 1) : imageValue;
      return Base64.getDecoder().decode(base64Part);
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  private boolean isTerminal(String status) {
    return GenerationStatus.COMPLETED.name().equals(status)
        || GenerationStatus.FAILED.name().equals(status)
        || GenerationStatus.CANCELED.name().equals(status)
        || GenerationStatus.EXPIRED.name().equals(status);
  }
}
