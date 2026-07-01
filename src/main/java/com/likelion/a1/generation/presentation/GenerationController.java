package com.likelion.a1.generation.presentation;

import com.likelion.a1.generation.application.GenerationCommandService;
import com.likelion.a1.generation.application.GenerationQueryService;
import com.likelion.a1.global.response.ApiResponse;
import com.likelion.a1.generation.presentation.dto.GenerationRequest;
import com.likelion.a1.generation.presentation.dto.GenerationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/generations")
public class GenerationController {
    private final GenerationCommandService commandService;
    private final GenerationQueryService queryService;

    public GenerationController(GenerationCommandService commandService, GenerationQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<GenerationResponse> create(@Valid @RequestBody GenerationRequest request) {
        return ApiResponse.success(GenerationResponse.from(
                commandService.create(request.userId(), request.type(), request.model(), request.prompt())));
    }

    @GetMapping("/{id}")
    ApiResponse<GenerationResponse> get(@PathVariable UUID id) {
        return ApiResponse.success(GenerationResponse.from(queryService.get(id)));
    }
}
