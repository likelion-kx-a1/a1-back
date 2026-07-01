package com.likelion.a1.generation.infrastructure.external;

import com.likelion.a1.generation.application.port.AiGenerationPort;
import com.likelion.a1.generation.domain.GenerationType;
import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import com.likelion.a1.generation.infrastructure.external.openai.OpenAiImageClient;
import com.likelion.a1.generation.infrastructure.external.fal.FalSeedanceClient;
import org.springframework.stereotype.Component;

@Component
public class AiGenerationRouter implements AiGenerationPort {
    private final OpenAiImageClient openAi;
    private final FalSeedanceClient fal;

    public AiGenerationRouter(OpenAiImageClient openAi, FalSeedanceClient fal) {
        this.openAi = openAi;
        this.fal = fal;
    }

    public Submission submit(GenerationType type, String model, String prompt) {
        return switch (type) {
            case IMAGE_GENERATION, IMAGE_VARIATION ->
                    Submission.completed(openAi.generate(type, model, prompt));
            case VIDEO_GENERATION -> Submission.pending(fal.submit(type, model, prompt));
            case REVERSE_PROMPT, PROMPT_REGENERATION ->
                    throw new BusinessException(ErrorCode.AI_PROVIDER_UNAVAILABLE);
        };
    }
}
