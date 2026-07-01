package com.likelion.a1.media.infrastructure.storage.s3;

import com.likelion.a1.media.application.port.MediaStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class S3MediaStorageAdapter implements MediaStoragePort {
    private final S3Client s3Client;
    private final String bucket;
    private final String publicBaseUrl;

    public S3MediaStorageAdapter(S3Client s3Client,
            @Value("${app.storage.bucket}") String bucket,
            @Value("${app.storage.public-base-url:}") String publicBaseUrl) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl;
    }

    @Override
    public String store(byte[] content, String contentType, String extension) {
        String key = "generated/" + LocalDate.now() + "/" + UUID.randomUUID() + "." + extension;
        s3Client.putObject(PutObjectRequest.builder()
                .bucket(bucket).key(key).contentType(contentType)
                .contentLength((long) content.length).build(), RequestBody.fromBytes(content));
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            return publicBaseUrl.replaceAll("/$", "") + "/" + key;
        }
        return "s3://" + bucket + "/" + key;
    }
}
