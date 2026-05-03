package com.team3.monew.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AwsProperties.class)
public class S3Config {

  private final AwsProperties awsProperties;

  @Bean
  public S3Client createS3Client() {
    if (awsProperties.getRegion() == null ||
        awsProperties.getRegion().getStaticRegion().isBlank()) {
      throw new IllegalArgumentException("AWS S3 region은 필수 설정값입니다. (cloud.aws.region)");
    }

    return S3Client.builder()
        .region(Region.of(awsProperties.getRegion().getStaticRegion()))
        .credentialsProvider(getCredentialsProvider())
        .build();
  }

  private AwsCredentialsProvider getCredentialsProvider() {
    String propertyAccessKey = getPropertyAccessKey();
    String propertySecretKey = getPropertySecretKey();
    String envAccessKey = System.getenv("AWS_ACCESS_KEY_ID");
    String envSecretKey = System.getenv("AWS_SECRET_ACCESS_KEY");
    String sessionToken = System.getenv("AWS_SESSION_TOKEN");

    if (hasText(propertyAccessKey) && hasText(propertySecretKey)) {
      return StaticCredentialsProvider.create(
          AwsBasicCredentials.create(propertyAccessKey, propertySecretKey));
    }

    if (hasText(envAccessKey) && hasText(envSecretKey)) {
      return sessionToken != null && !sessionToken.isBlank()
          ? StaticCredentialsProvider.create(
          AwsSessionCredentials.create(envAccessKey, envSecretKey, sessionToken))
          : StaticCredentialsProvider.create(
              AwsBasicCredentials.create(envAccessKey, envSecretKey));
    }

    return DefaultCredentialsProvider.create();
  }

  private String getPropertyAccessKey() {
    return awsProperties.getCredentials() == null
        ? null
        : awsProperties.getCredentials().getAccessKey();
  }

  private String getPropertySecretKey() {
    return awsProperties.getCredentials() == null
        ? null
        : awsProperties.getCredentials().getSecretKey();
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
