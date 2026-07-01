package com.likelion.a1.generation.presentation.dto;

import com.likelion.a1.generation.domain.*;
import java.time.OffsetDateTime;
import java.util.UUID;

public record GenerationResponse(
        UUID id, GenerationType type, String model, GenerationStatus status,
        String providerRequestId, String resultUrl, String failureMessage,
        OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
    public static GenerationResponse from(GenerationJob job) {
        return new GenerationResponse(job.getPublicId(), job.getType(), job.getModel(), job.getStatus(),
                job.getProviderRequestId(), job.getResultUrl(), job.getFailureMessage(),
                job.getCreatedAt(), job.getUpdatedAt());
    }
}
