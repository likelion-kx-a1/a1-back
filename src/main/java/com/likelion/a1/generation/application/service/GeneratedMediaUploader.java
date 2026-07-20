package com.likelion.a1.generation.application.service;

import com.likelion.a1.generation.domain.model.GenerationJob;
import com.likelion.a1.generation.domain.model.GenerationStatus;
import com.likelion.a1.generation.domain.model.GenerationType;
import com.likelion.a1.media.application.port.out.MediaStoragePort;
import com.likelion.a1.media.application.port.out.StorageUploadCommand;
import com.likelion.a1.media.application.port.out.StorageUploadResult;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * fal.ai가 COMPLETED로 응답한 임시 미디어 URL을 S3(MediaStoragePort)로 영구 이관한다.
 * {@link GenerationVideoPollingScheduler}(백그라운드 배치)와 {@link GenerationAiService#getStatus}
 * (수동 폴링 API)가 이 로직을 공유해야, 어느 쪽이 먼저 COMPLETED를 감지하든 항상 S3 업로드를 거치고
 * 완료 처리된다.
 */
@Component
class GeneratedMediaUploader {
  private final MediaStoragePort mediaStoragePort;
  private final RestTemplate restTemplate = new RestTemplate();

  GeneratedMediaUploader(MediaStoragePort mediaStoragePort) {
    this.mediaStoragePort = mediaStoragePort;
  }

  /**
   * status가 COMPLETED이고 payload에 업로드할 미디어 URL이 있으면 S3로 업로드하고 payload에
   * {@code s3Url}을 채운다. 업로드에 실패하면 FAILED를 반환한다. 그 외에는 입력받은 status를 그대로 반환한다.
   * payload는 호출자가 전달한 맵을 그대로 변형(mutate)한다.
   */
  GenerationStatus applyCompletion(GenerationJob job, GenerationStatus status, Map<String, Object> payload) {
    if (status != GenerationStatus.COMPLETED) {
      return status;
    }

    String temporaryUrl = extractMediaUrl(payload);
    if (temporaryUrl == null) {
      return status;
    }

    try {
      payload.put("s3Url", uploadToS3(job, temporaryUrl));

      long asyncPollingDurationSec = Duration.between(job.getStartedAt(), OffsetDateTime.now()).getSeconds();
      PerformanceMetrics.record(payload, "asyncPollingDurationSec", asyncPollingDurationSec);
      PerformanceMetrics.announce(job.getId(), payload);

      return status;
    } catch (RuntimeException exception) {
      return GenerationStatus.FAILED;
    }
  }

  private String extractMediaUrl(Map<String, Object> payload) {
    if (payload.get("video") instanceof Map<?, ?> video && video.get("url") instanceof String url) {
      return url;
    }
    if (payload.get("images") instanceof List<?> images
        && !images.isEmpty()
        && images.get(0) instanceof Map<?, ?> firstImage
        && firstImage.get("url") instanceof String url) {
      return url;
    }
    return null;
  }

  private String uploadToS3(GenerationJob job, String temporaryUrl) {
    boolean isVideo = GenerationType.VIDEO_GENERATION.name().equals(job.getGenerationType());
    String extension = isVideo ? "mp4" : "png";
    String contentType = isVideo ? "video/mp4" : "image/png";
    String directory =
        "users/" + job.getUserId() + "/chats/" + job.getChatId() + "/generated/" + (isVideo ? "videos" : "images");

    StorageUploadResult result =
        mediaStoragePort.upload(
            new StorageUploadCommand(
                downloadBytes(temporaryUrl), "generated." + extension, contentType, extension, directory));

    return result.publicUrl();
  }

  private byte[] downloadBytes(String url) {
    if (url.startsWith("data:")) {
      String base64 = url.substring(url.indexOf(',') + 1);
      return Base64.getDecoder().decode(base64);
    }
    return restTemplate.getForObject(url, byte[].class);
  }
}
