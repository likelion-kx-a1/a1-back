package com.likelion.a1.media.infrastructure.storage.s3;

import com.likelion.a1.media.application.port.out.MediaStoragePort;
import com.likelion.a1.media.application.port.out.StorageUploadCommand;
import com.likelion.a1.media.application.port.out.StorageUploadResult;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
public class S3MediaStorageAdapter implements MediaStoragePort {
  private final S3Client s3Client;
  private final String bucket;
  private final String publicBaseUrl;

  public S3MediaStorageAdapter(
      S3Client s3Client,
      @Value("${app.storage.bucket}") String bucket,
      @Value("${app.storage.public-base-url:}") String publicBaseUrl) {
    this.s3Client = s3Client;
    this.bucket = bucket;
    this.publicBaseUrl = publicBaseUrl;
  }

  @Override
  public StorageUploadResult upload(StorageUploadCommand command) {
    String extension = normalizeExtension(command.extension(), command.originalFilename());
    String directory = normalizeDirectory(command.directory());
    String storedFilename = UUID.randomUUID() + "." + extension;
    String storagePath = directory + "/" + LocalDate.now() + "/" + storedFilename;

    s3Client.putObject(
        PutObjectRequest.builder()
            .bucket(bucket)
            .key(storagePath)
            .contentType(command.contentType())
            .contentLength((long) command.content().length)
            .build(),
        RequestBody.fromBytes(command.content()));

    return new StorageUploadResult(
        bucket,
        storagePath,
        resolvePublicUrl(storagePath),
        command.originalFilename(),
        storedFilename,
        command.contentType(),
        command.content().length);
  }

  private String resolvePublicUrl(String storagePath) {
    if (StringUtils.hasText(publicBaseUrl)) {
      return publicBaseUrl.replaceAll("/$", "") + "/" + storagePath;
    }

    return "s3://" + bucket + "/" + storagePath;
  }

  private String normalizeDirectory(String directory) {
    if (!StringUtils.hasText(directory)) {
      return "uploads";
    }

    return directory.trim().replaceAll("^/+", "").replaceAll("/+$", "");
  }

  private String normalizeExtension(String extension, String originalFilename) {
    if (StringUtils.hasText(extension)) {
      return extension.trim().replace(".", "").toLowerCase();
    }

    if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
      return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
    }

    return "bin";
  }
}
