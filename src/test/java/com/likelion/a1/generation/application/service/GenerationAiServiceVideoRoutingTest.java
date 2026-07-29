package com.likelion.a1.generation.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.likelion.a1.generation.application.port.out.AiTextGenerationResult;
import com.likelion.a1.generation.application.port.out.CharacterSheetPromptPort;
import com.likelion.a1.generation.application.port.out.FalGenerationPort;
import com.likelion.a1.generation.application.port.out.FalGenerationSubmission;
import com.likelion.a1.generation.application.port.out.ImageAnalysisPort;
import com.likelion.a1.generation.application.port.out.PromptGenerationPort;
import com.likelion.a1.generation.domain.model.GenerationJob;
import com.likelion.a1.generation.domain.repository.GenerationJobRepository;
import com.likelion.a1.global.exception.BusinessException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GenerationAiServiceVideoRoutingTest {
  private GenerationJobRepository generationJobRepository;
  private FalGenerationPort falGenerationPort;
  private PromptGenerationPort promptGenerationPort;
  private GenerationAiService service;

  @BeforeEach
  void setUp() {
    generationJobRepository = mock(GenerationJobRepository.class);
    falGenerationPort = mock(FalGenerationPort.class);
    promptGenerationPort = mock(PromptGenerationPort.class);
    GenerationResultService generationResultService = mock(GenerationResultService.class);

    when(generationJobRepository.save(any(GenerationJob.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(falGenerationPort.submit(any(), any()))
        .thenReturn(
            new FalGenerationSubmission(
                "request-id", "https://fal/status", "https://fal/result", Map.of()));

    service =
        new GenerationAiService(
            generationJobRepository,
            promptGenerationPort,
            mock(ImageAnalysisPort.class),
            falGenerationPort,
            mock(CharacterSheetPromptPort.class),
            mock(GeneratedMediaUploader.class),
            generationResultService);
  }

  @Test
  void seedanceWithOneImageUsesReferenceToVideo() {
    generate(true, List.of("image-1"));

    verify(falGenerationPort)
        .submit(eq("bytedance/seedance-2.0/reference-to-video"), any());
  }

  @Test
  void klingWithOneImageUsesReferenceToVideo() {
    generate(false, List.of("image-1"));

    verify(falGenerationPort)
        .submit(eq("fal-ai/kling-video/o3/standard/reference-to-video"), any());
  }

  @Test
  void klingKeepsStartEndAndReferenceImageRolesSeparate() {
    service.generateVideo(
        1L,
        2L,
        false,
        List.of("start", "end", "reference"),
        "start",
        "end",
        List.of("reference"),
        "prompt",
        5,
        "16:9",
        "720p",
        false,
        3L);

    verify(falGenerationPort)
        .submit(
            eq("fal-ai/kling-video/o3/standard/reference-to-video"),
            org.mockito.ArgumentMatchers.argThat(
                input ->
                    "start".equals(input.get("start_image_url"))
                        && "end".equals(input.get("end_image_url"))
                        && List.of("reference").equals(input.get("images"))
                        && "720p".equals(input.get("resolution"))));
  }

  @Test
  void klingWithStartFrameOnlyDoesNotDuplicateItAsReferenceImage() {
    service.generateVideo(
        1L,
        2L,
        false,
        List.of("start"),
        "start",
        null,
        null,
        "prompt",
        5,
        "16:9",
        "720p",
        false,
        3L);

    verify(falGenerationPort)
        .submit(
            eq("fal-ai/kling-video/o3/standard/reference-to-video"),
            org.mockito.ArgumentMatchers.argThat(
                input ->
                    "start".equals(input.get("start_image_url"))
                        && !input.containsKey("images")));
  }

  @Test
  void klingRejectsEndFrameWithoutStartFrame() {
    assertThatThrownBy(
            () ->
                service.generateVideo(
                    1L,
                    2L,
                    false,
                    List.of("end"),
                    null,
                    "end",
                    null,
                    "prompt",
                    5,
                    "16:9",
                    "720p",
                    false,
                    3L))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void seedancePassesResolutionAndReferenceLabelsToRefinement() {
    when(promptGenerationPort.generateFromImage(any(), eq("image/png"), any()))
        .thenReturn(new AiTextGenerationResult("Use @Image1 and @Image2.", Map.of()));

    service.generateVideo(
        1L,
        2L,
        true,
        List.of("one", "two"),
        null,
        null,
        List.of("one", "two"),
        "prompt",
        5,
        "16:9",
        "1080p",
        true,
        3L);

    verify(promptGenerationPort)
        .generateFromImage(
            any(),
            eq("image/png"),
            org.mockito.ArgumentMatchers.argThat(
                instruction ->
                    instruction.contains("@Image1") && instruction.contains("@Image2")));
    verify(falGenerationPort)
        .submit(
            eq("bytedance/seedance-2.0/reference-to-video"),
            org.mockito.ArgumentMatchers.argThat(
                input ->
                    "1080p".equals(input.get("resolution"))
                        && List.of("one", "two").equals(input.get("images"))));
  }

  @Test
  void klingRejectsMoreThanFourReferenceImages() {
    assertThatThrownBy(() -> generate(false, List.of("1", "2", "3", "4", "5")))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void klingRefinedPromptIsLimitedBeforeSubmission() {
    String longPrompt = "A detailed cinematic action with consistent identity. ".repeat(80);
    when(promptGenerationPort.generateFromImage(any(), eq("image/png"), any()))
        .thenReturn(new AiTextGenerationResult(longPrompt, Map.of()));

    GenerationJob job =
        service.generateVideo(
            1L, 2L, false, List.of("image-1"), "prompt", 5, "16:9", true, 3L);

    verify(falGenerationPort)
        .submit(
            eq("fal-ai/kling-video/o3/standard/reference-to-video"),
            org.mockito.ArgumentMatchers.argThat(
                input -> {
                  String submittedPrompt = (String) input.get("prompt");
                  return submittedPrompt.codePointCount(0, submittedPrompt.length()) <= 2400;
                }));
    org.assertj.core.api.Assertions.assertThat(job.getResponsePayload().get("promptTruncated"))
        .isEqualTo(true);
    org.assertj.core.api.Assertions.assertThat(job.getResponsePayload().get("refinedPrompt"))
        .isEqualTo(longPrompt);
  }

  private void generate(boolean highQuality, List<String> images) {
    service.generateVideo(
        1L, 2L, highQuality, images, "prompt", 5, "16:9", false, 3L);
  }
}
