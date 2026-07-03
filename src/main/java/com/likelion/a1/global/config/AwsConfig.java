package com.likelion.a1.global.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
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
      @Value("${app.storage.path-style-access:false}") boolean pathStyleAccess) {
    S3ClientBuilder builder =
        S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .serviceConfiguration(
                S3Configuration.builder().pathStyleAccessEnabled(pathStyleAccess).build());
    if (endpoint != null && !endpoint.isBlank()) builder.endpointOverride(URI.create(endpoint));
    return builder.build();
  }
}
