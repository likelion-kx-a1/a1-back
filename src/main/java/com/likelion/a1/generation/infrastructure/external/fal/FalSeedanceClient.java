package com.likelion.a1.generation.infrastructure.external.fal;

import com.likelion.a1.generation.application.port.VideoStatusPort;
import com.likelion.a1.generation.domain.GenerationType;
import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import com.likelion.a1.media.application.port.MediaStoragePort;

import java.util.Map;

@Component
public class FalSeedanceClient implements VideoStatusPort {
    private final WebClient client;
    private final String apiKey;
    private final String defaultModel;
    private final MediaStoragePort storage;

    public FalSeedanceClient(WebClient.Builder builder, MediaStoragePort storage,
            @Value("${app.fal.queue-base-url}") String baseUrl,
            @Value("${app.fal.api-key}") String apiKey,
            @Value("${app.fal.video-model}") String defaultModel) {
        this.client = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.defaultModel = defaultModel;
        this.storage = storage;
    }

    public String submit(GenerationType type, String model, String prompt) {
        requireKey();
        String endpoint = model == null || model.isBlank() ? defaultModel : model;
        FalSubmitResponse response = client.post().uri("/" + endpoint)
                .header("Authorization", "Key " + apiKey)
                .bodyValue(Map.of("prompt", prompt))
                .retrieve().bodyToMono(FalSubmitResponse.class).block();
        String requestId = response == null ? null : response.request_id();
        if (requestId == null || requestId.isBlank())
            throw new IllegalStateException("fal.ai response did not contain request_id");
        return requestId;
    }

    private void requireKey() {
        if (apiKey == null || apiKey.isBlank())
            throw new BusinessException(ErrorCode.AI_PROVIDER_UNAVAILABLE);
    }

    @Override
    public Result poll(String model, String requestId) {
        requireKey();
        String endpoint = model == null || model.isBlank() ? defaultModel : model;
        FalStatusResponse status = client.get()
                .uri("/" + endpoint + "/requests/" + requestId + "/status")
                .header("Authorization", "Key " + apiKey)
                .retrieve().bodyToMono(FalStatusResponse.class).block();
        if (status == null || !"COMPLETED".equals(status.status())) {
            return new Result(State.PROCESSING, null);
        }

        FalResultResponse result = client.get()
                .uri("/" + endpoint + "/requests/" + requestId)
                .header("Authorization", "Key " + apiKey)
                .retrieve().bodyToMono(FalResultResponse.class).block();
        if (result == null || result.video() == null || result.video().url() == null) {
            throw new IllegalStateException("fal.ai result did not contain video.url");
        }
        byte[] video = WebClient.create().get().uri(result.video().url())
                .retrieve().bodyToMono(byte[].class).block();
        if (video == null || video.length == 0) {
            throw new IllegalStateException("fal.ai video download returned empty content");
        }
        return new Result(State.COMPLETED, storage.store(video, "video/mp4", "mp4"));
    }

    private record FalSubmitResponse(String request_id) {}
    private record FalStatusResponse(String status) {}
    private record FalResultResponse(VideoFile video) {}
    private record VideoFile(String url) {}
}
