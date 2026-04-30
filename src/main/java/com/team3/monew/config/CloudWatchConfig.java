package com.team3.monew.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AwsProperties.class)
public class CloudWatchConfig {

  private final AwsProperties awsProperties;

  @Bean
  public CloudWatchAsyncClient cloudWatchAsyncClient() {
    if (awsProperties.getRegion() == null ||
        awsProperties.getRegion().getStaticRegion().isBlank()) {
      throw new IllegalArgumentException("CloudWatch region은 필수 설정값입니다. (cloud.aws.region)");
    }

    return CloudWatchAsyncClient.builder()
        .region(Region.of(awsProperties.getRegion().getStaticRegion()))
        .credentialsProvider(getCredentialsProvider())
        .build();
  }

  private AwsCredentialsProvider getCredentialsProvider() {
    String accessKey = awsProperties.getCredentials().getAccessKey();
    String secretKey = awsProperties.getCredentials().getSecretKey();

    return accessKey != null && !accessKey.isBlank()
        && secretKey != null && !secretKey.isBlank()
        ? StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
        : DefaultCredentialsProvider.create();
  }
}
