package com.likelion.a1.generation.infrastructure.external.openai;

import com.likelion.a1.generation.domain.GenerationType;
import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import com.likelion.a1.media.application.port.MediaStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiImageClient {
    private final WebClient client;
    private final MediaStoragePort storage;
    private final String apiKey;
    private final String defaultModel;

    public OpenAiImageClient(WebClient.Builder builder, MediaStoragePort storage,
            @Value("${app.openai.base-url}") String baseUrl,
            @Value("${app.openai.api-key}") String apiKey,
            @Value("${app.openai.image-model}") String defaultModel) {
        this.client = builder.baseUrl(baseUrl).build();
        this.storage = storage;
        this.apiKey = apiKey;
        this.defaultModel = defaultModel;
    }

    public String generate(GenerationType type, String model, String prompt) {
        requireKey();
        String requestedModel = model == null || model.isBlank() ? defaultModel : model;
        ImageResponse response = client.post().uri("/v1/images/generations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .bodyValue(Map.of("model", requestedModel, "prompt", prompt,
                        "size", "1024x1024", "quality", "medium", "output_format", "png"))
                .retrieve().bodyToMono(ImageResponse.class).block();
        String image = response == null || response.data() == null || response.data().isEmpty()
                ? null : response.data().getFirst().b64_json();
        if (image == null || image.isBlank()) {
            throw new IllegalStateException("OpenAI image response did not contain b64_json");
        }
        byte[] bytes = Base64.getDecoder().decode(image);
        return storage.store(bytes, "image/png", "png");
    }

    private void requireKey() {
        if (apiKey == null || apiKey.isBlank())
            throw new BusinessException(ErrorCode.AI_PROVIDER_UNAVAILABLE);
    }

    private record ImageResponse(List<ImageData> data) {}
    private record ImageData(String b64_json) {}
}
