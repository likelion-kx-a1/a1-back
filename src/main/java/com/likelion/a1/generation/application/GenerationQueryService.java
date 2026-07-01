package com.likelion.a1.generation.application;

import com.likelion.a1.generation.domain.GenerationJob;
import com.likelion.a1.generation.domain.GenerationJobRepository;
import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GenerationQueryService {
    private final GenerationJobRepository repository;

    public GenerationQueryService(GenerationJobRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public GenerationJob get(UUID id) {
        return repository.findByPublicId(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.GENERATION_NOT_FOUND));
    }
}
