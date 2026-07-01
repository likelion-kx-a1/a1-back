package com.likelion.a1.generation.presentation.dto;

import com.likelion.a1.generation.domain.GenerationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GenerationRequest(
        @NotNull Long userId,
        @NotNull GenerationType type,
        @NotBlank @Size(max = 100) String model,
        @NotBlank @Size(max = 10000) String prompt
) {}
