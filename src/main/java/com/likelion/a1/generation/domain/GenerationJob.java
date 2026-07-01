package com.likelion.a1.generation.domain;

import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "generation_jobs")
public class GenerationJob {
    @Id @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, updatable = false)
    private UUID publicId;
    @Column(nullable = false)
    private Long userId;
    private Long aiModelId;
    @Enumerated(EnumType.STRING) @Column(name = "job_type", nullable = false, length = 50)
    private com.likelion.a1.generation.domain.GenerationType type;
    @Column(nullable = false, length = 20)
    private String mediaType;
    @Column(nullable = false, length = 50)
    private String provider;
    @Column(name = "model_name", nullable = false, length = 100)
    private String model;
    @Column(nullable = false, columnDefinition = "text")
    private String prompt;
    @Column(columnDefinition = "text")
    private String negativePrompt;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb")
    private Map<String, Object> requestPayload;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb")
    private Map<String, Object> responsePayload;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private GenerationStatus status;
    @Column(name = "external_request_id")
    private String providerRequestId;
    private String externalStatus;
    @Column(nullable = false)
    private int progress;
    @Column(nullable = false)
    private int retryCount;
    @Column(nullable = false)
    private int maxRetryCount;
    private OffsetDateTime nextPollAt;
    private OffsetDateTime lastPolledAt;
    @Column(nullable = false)
    private int pollCount;
    private String errorCode;
    @Column(columnDefinition = "text")
    private String errorMessage;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private OffsetDateTime failedAt;
    @Column(nullable = false)
    private OffsetDateTime createdAt;
    @Column(nullable = false)
    private OffsetDateTime updatedAt;
    @Version private long version;

    protected GenerationJob() {}

    private GenerationJob(Long userId, com.likelion.a1.generation.domain.GenerationType type,
            String provider, String model, String prompt) {
        this.publicId = UUID.randomUUID();
        this.userId = userId;
        this.type = type;
        this.mediaType = type.mediaType();
        this.provider = provider;
        this.model = model;
        this.prompt = prompt;
        this.status = GenerationStatus.PENDING;
        this.progress = 0;
        this.retryCount = 0;
        this.maxRetryCount = 3;
        this.pollCount = 0;
        this.requestPayload = Map.of("prompt", prompt);
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = createdAt;
    }

    public static GenerationJob create(Long userId,
            com.likelion.a1.generation.domain.GenerationType type,
            String provider, String model, String prompt) {
        return new GenerationJob(userId, type, provider, model, prompt);
    }

    public void start(String requestId) {
        requireStatus(GenerationStatus.PENDING);
        providerRequestId = requestId;
        status = requestId == null ? GenerationStatus.PROCESSING : GenerationStatus.QUEUED;
        externalStatus = requestId == null ? "PROCESSING" : "IN_QUEUE";
        startedAt = OffsetDateTime.now();
        nextPollAt = requestId == null ? null : OffsetDateTime.now().plusSeconds(15);
        touch();
    }

    public void markProcessing() {
        if (status != GenerationStatus.QUEUED && status != GenerationStatus.PROCESSING)
            throw new BusinessException(ErrorCode.INVALID_GENERATION_STATE);
        status = GenerationStatus.PROCESSING;
        externalStatus = "IN_PROGRESS";
        lastPolledAt = OffsetDateTime.now();
        nextPollAt = lastPolledAt.plusSeconds(15);
        pollCount++;
        touch();
    }

    public void complete(String url) {
        if (status != GenerationStatus.PENDING && status != GenerationStatus.QUEUED
                && status != GenerationStatus.PROCESSING)
            throw new BusinessException(ErrorCode.INVALID_GENERATION_STATE);
        responsePayload = new HashMap<>();
        responsePayload.put("resultUrl", url);
        status = GenerationStatus.COMPLETED;
        externalStatus = "COMPLETED";
        progress = 100;
        completedAt = OffsetDateTime.now();
        nextPollAt = null;
        touch();
    }

    public void fail(String message) {
        if (status == GenerationStatus.COMPLETED)
            throw new BusinessException(ErrorCode.INVALID_GENERATION_STATE);
        errorMessage = message;
        status = GenerationStatus.FAILED;
        failedAt = OffsetDateTime.now();
        nextPollAt = null;
        touch();
    }

    private void requireStatus(GenerationStatus expected) {
        if (status != expected) throw new BusinessException(ErrorCode.INVALID_GENERATION_STATE);
    }
    private void touch() { updatedAt = OffsetDateTime.now(); }

    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public Long getUserId() { return userId; }
    public com.likelion.a1.generation.domain.GenerationType getType() { return type; }
    public String getModel() { return model; }
    public String getPrompt() { return prompt; }
    public GenerationStatus getStatus() { return status; }
    public String getProviderRequestId() { return providerRequestId; }
    public String getResultUrl() {
        return responsePayload == null ? null : (String) responsePayload.get("resultUrl");
    }
    public String getFailureMessage() { return errorMessage; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
