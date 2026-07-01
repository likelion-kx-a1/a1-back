package com.likelion.a1.generation.application;

import com.likelion.a1.generation.application.port.AiGenerationPort;
import com.likelion.a1.generation.domain.GenerationJob;
import com.likelion.a1.generation.domain.GenerationJobRepository;
import com.likelion.a1.generation.domain.GenerationType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;

@Service
public class GenerationCommandService {
    private final GenerationJobRepository repository;
    private final AiGenerationPort aiPort;
    private final TransactionTemplate transaction;

    public GenerationCommandService(
            GenerationJobRepository repository,
            AiGenerationPort aiPort,
            TransactionTemplate transaction
    ) {
        this.repository = repository;
        this.aiPort = aiPort;
        this.transaction = transaction;
    }

    public GenerationJob create(Long userId, GenerationType type, String model, String prompt) {
        GenerationJob job = Objects.requireNonNull(transaction.execute(
                ignored -> repository.save(GenerationJob.create(userId, type, model, prompt))));
        try {
            AiGenerationPort.Submission submission = aiPort.submit(type, model, prompt);
            return Objects.requireNonNull(transaction.execute(
                    ignored -> applySubmission(job.getId(), submission)));
        } catch (RuntimeException exception) {
            transaction.executeWithoutResult(ignored -> markFailed(job.getId(), exception.getMessage()));
            throw exception;
        }
    }

    private GenerationJob applySubmission(Long id, AiGenerationPort.Submission result) {
        GenerationJob job = repository.findById(id).orElseThrow();
        if (result.completed()) job.complete(result.resultUrl());
        else job.start(result.providerRequestId());
        return job;
    }

    private void markFailed(Long id, String message) {
        repository.findById(id).ifPresent(job -> job.fail(message));
    }
}
