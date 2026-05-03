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
    String accessKey = firstNonBlank(
        awsProperties.getCredentials().getAccessKey(),
        System.getenv("AWS_ACCESS_KEY_ID")
    );
    String secretKey = firstNonBlank(
        awsProperties.getCredentials().getSecretKey(),
        System.getenv("AWS_SECRET_ACCESS_KEY")
    );
    String sessionToken = System.getenv("AWS_SESSION_TOKEN");

    if (accessKey != null && !accessKey.isBlank()
        && secretKey != null && !secretKey.isBlank()) {
      return sessionToken != null && !sessionToken.isBlank()
          ? StaticCredentialsProvider.create(
          AwsSessionCredentials.create(accessKey, secretKey, sessionToken))
          : StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }

    return DefaultCredentialsProvider.create();
  }

  private String firstNonBlank(String first, String second) {
    return first != null && !first.isBlank() ? first : second;
  }
}
