package com.likelion.a1.media.infrastructure.storage.s3;

import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import com.likelion.a1.media.application.port.out.MediaStoragePort;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

@Component
@Profile("!local")
public class S3MediaStorageAdapter implements MediaStoragePort {
  private static final int PART_SIZE_BYTES = 8 * 1024 * 1024;

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
  public String store(byte[] content, String contentType, String extension) {
    String key = generateKey(extension);
    s3Client.putObject(
        PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(contentType)
            .contentLength((long) content.length)
            .build(),
        RequestBody.fromBytes(content));
    return buildPublicUrl(key);
  }

  @Override
  public String storeFromUrl(String temporaryUrl, String contentType, String extension) {
    String key = generateKey(extension);
    String uploadId =
        s3Client
            .createMultipartUpload(
                CreateMultipartUploadRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .build())
            .uploadId();

    List<CompletedPart> completedParts = new ArrayList<>();
    try (InputStream sourceStream = URI.create(temporaryUrl).toURL().openStream()) {
      byte[] buffer = new byte[PART_SIZE_BYTES];
      int partNumber = 1;
      int bytesRead;
      while ((bytesRead = readFully(sourceStream, buffer)) > 0) {
        String eTag =
            s3Client
                .uploadPart(
                    UploadPartRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .uploadId(uploadId)
                        .partNumber(partNumber)
                        .build(),
                    RequestBody.fromByteBuffer(ByteBuffer.wrap(buffer, 0, bytesRead)))
                .eTag();
        completedParts.add(CompletedPart.builder().partNumber(partNumber).eTag(eTag).build());
        partNumber++;
      }
      s3Client.completeMultipartUpload(
          CompleteMultipartUploadRequest.builder()
              .bucket(bucket)
              .key(key)
              .uploadId(uploadId)
              .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build())
              .build());
    } catch (IOException e) {
      s3Client.abortMultipartUpload(
          AbortMultipartUploadRequest.builder().bucket(bucket).key(key).uploadId(uploadId).build());
      throw new BusinessException(ErrorCode.MEDIA_UPLOAD_FAILED);
    }
    return buildPublicUrl(key);
  }

  private int readFully(InputStream in, byte[] buffer) throws IOException {
    int total = 0;
    while (total < buffer.length) {
      int read = in.read(buffer, total, buffer.length - total);
      if (read == -1) break;
      total += read;
    }
    return total;
  }

  private String generateKey(String extension) {
    return "generated/" + LocalDate.now() + "/" + UUID.randomUUID() + "." + extension;
  }

  private String buildPublicUrl(String key) {
    if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
      return publicBaseUrl.replaceAll("/$", "") + "/" + key;
    }
    return "s3://" + bucket + "/" + key;
  }
}
