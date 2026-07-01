package com.likelion.a1.generation.domain;

import com.likelion.a1.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class GenerationJobTest {
    @Test
    void pendingJobCanStartAndComplete() {
        GenerationJob job = GenerationJob.create(1L, GenerationType.VIDEO_GENERATION, "seedance", "city at night");
        job.start("provider-1");
        job.complete("https://storage/result.mp4");
        assertThat(job.getStatus()).isEqualTo(GenerationStatus.COMPLETED);
    }

    @Test
    void completedJobCannotFail() {
        GenerationJob job = GenerationJob.create(1L, GenerationType.IMAGE_GENERATION, "openai", "studio");
        job.complete("https://storage/result.png");
        assertThatThrownBy(() -> job.fail("late error")).isInstanceOf(BusinessException.class);
    }
}
