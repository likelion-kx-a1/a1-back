package com.likelion.a1.generation.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.likelion.a1.generation.application.service.GenerationAiService;
import com.likelion.a1.generation.domain.model.GenerationJob;
import com.likelion.a1.generation.domain.model.GenerationType;
import com.likelion.a1.global.response.ApiResponse;
import com.likelion.a1.user.infrastructure.security.JwtPrincipal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.OptimisticLockingFailureException;

class GenerationControllerConcurrencyTest {

  @Test
  void retriesStatusLookupInANewServiceCallAfterOptimisticLockConflict() {
    GenerationAiService generationAiService = Mockito.mock(GenerationAiService.class);
    GenerationController controller = new GenerationController(generationAiService);
    GenerationJob completedJob =
        GenerationJob.create(
            7L,
            118L,
            null,
            301L,
            GenerationType.VIDEO_GENERATION.name(),
            "test video",
            Map.of("modelCode", "test/model"));
    completedJob.complete(Map.of("s3Url", "https://cdn.example/video.mp4"));

    when(generationAiService.getStatus(9L, 7L))
        .thenThrow(new OptimisticLockingFailureException("simulated concurrent update"))
        .thenReturn(completedJob);

    ApiResponse<?> response =
        controller.getFalJobStatus(
            new JwtPrincipal(7L, "hyeon8260", "USER", "test-session"), 9L);

    assertThat(response).isNotNull();
    verify(generationAiService, times(2)).getStatus(9L, 7L);
  }
}
