package com.likelion.a1.generation.application;

import com.likelion.a1.generation.application.port.VideoStatusPort;
import com.likelion.a1.generation.domain.GenerationJob;
import com.likelion.a1.generation.domain.GenerationJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;

@Service
public class VideoPollingService {
    private static final Logger log = LoggerFactory.getLogger(VideoPollingService.class);
    private final GenerationJobRepository repository;
    private final VideoStatusPort statusPort;
    private final TransactionTemplate transaction;

    public VideoPollingService(GenerationJobRepository repository,
            VideoStatusPort statusPort, TransactionTemplate transaction) {
        this.repository = repository;
        this.statusPort = statusPort;
        this.transaction = transaction;
    }

    @Scheduled(fixedDelayString = "${app.fal.poll-interval:15000}")
    public void poll() {
        repository.findProcessingVideos().forEach(this::pollOne);
    }

    private void pollOne(GenerationJob job) {
        try {
            VideoStatusPort.Result result = statusPort.poll(job.getModel(), job.getProviderRequestId());
            if (result.state() == VideoStatusPort.State.COMPLETED) {
                transaction.executeWithoutResult(ignored -> {
                    GenerationJob current = Objects.requireNonNull(repository.findById(job.getId()).orElse(null));
                    current.complete(result.storedUrl());
                });
            }
        } catch (RuntimeException exception) {
            log.warn("Video polling failed. jobId={}", job.getId(), exception);
        }
    }
}
