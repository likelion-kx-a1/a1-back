package com.likelion.a1.global.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
public class AwsConfig {
  @Bean
  S3Client s3Client(
      @Value("${app.storage.region}") String region,
      @Value("${app.storage.endpoint:}") String endpoint,
      @Value("${app.storage.path-style-access:false}") boolean pathStyleAccess,
      @Value("${app.storage.access-key:}") String accessKey,
      @Value("${app.storage.secret-key:}") String secretKey) {
    S3ClientBuilder builder =
        S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(credentialsProvider(accessKey, secretKey))
            .serviceConfiguration(
                S3Configuration.builder().pathStyleAccessEnabled(pathStyleAccess).build());

    if (StringUtils.hasText(endpoint)) {
      builder.endpointOverride(URI.create(endpoint));
    }

    return builder.build();
  }

  private AwsCredentialsProvider credentialsProvider(String accessKey, String secretKey) {
    if (StringUtils.hasText(accessKey) && StringUtils.hasText(secretKey)) {
      return StaticCredentialsProvider.create(
          AwsBasicCredentials.create(accessKey.trim(), secretKey.trim()));
    }

    return DefaultCredentialsProvider.create();
  }
}
