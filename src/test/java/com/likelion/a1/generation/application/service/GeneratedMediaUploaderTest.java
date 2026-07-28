package com.likelion.a1.generation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.likelion.a1.generation.domain.model.GenerationJob;
import com.likelion.a1.generation.domain.model.GenerationStatus;
import com.likelion.a1.generation.domain.model.GenerationType;
import com.likelion.a1.generation.domain.repository.GenerationJobRepository;
import com.likelion.a1.media.application.port.out.MediaStoragePort;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GeneratedMediaUploaderTest {

  @Test
  void skipsExternalSideEffectsWhenAnotherPollerAlreadyFinalizedTheJob() {
    MediaStoragePort mediaStoragePort = mock(MediaStoragePort.class);
    GenerationResultService generationResultService = mock(GenerationResultService.class);
    GenerationJobRepository generationJobRepository = mock(GenerationJobRepository.class);
    GeneratedMediaUploader uploader =
        new GeneratedMediaUploader(
            mediaStoragePort, generationResultService, generationJobRepository);

    GenerationJob staleJob = videoJob();
    GenerationJob finalizedJob = videoJob();
    finalizedJob.complete(Map.of("s3Url", "https://cdn.example/video.mp4"));
    when(generationJobRepository.findById(staleJob.getId()))
        .thenReturn(Optional.of(finalizedJob));

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("video", Map.of("url", "https://fal.example/temporary.mp4"));

    GenerationStatus result =
        uploader.applyCompletion(staleJob, GenerationStatus.COMPLETED, payload);

    assertThat(result).isEqualTo(GenerationStatus.COMPLETED);
    assertThat(payload).doesNotContainKey("s3Url");
    verifyNoInteractions(mediaStoragePort, generationResultService);
  }

  private GenerationJob videoJob() {
    return GenerationJob.create(
        7L,
        118L,
        null,
        301L,
        GenerationType.VIDEO_GENERATION.name(),
        "test video",
        Map.of("modelCode", "test/model"));
  }
}
