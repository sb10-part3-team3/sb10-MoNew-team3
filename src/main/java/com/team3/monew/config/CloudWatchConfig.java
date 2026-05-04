package com.team3.monew.config;

import lombok.RequiredArgsConstructor;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AwsProperties.class)
public class CloudWatchConfig {

  private final AwsProperties awsProperties;
  @Value("${management.metrics.export.cloudwatch.namespace:monew}")
  private String namespace;
  @Value("${management.metrics.export.cloudwatch.step:1m}")
  private String step;

  @Bean
  @ConditionalOnProperty(
      name = "management.metrics.export.cloudwatch.enabled",
      havingValue = "true"
  )
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

  @Bean
  @ConditionalOnProperty(
      name = "management.metrics.export.cloudwatch.enabled",
      havingValue = "true"
  )
  public CloudWatchMeterRegistry cloudWatchMeterRegistry(
      CloudWatchAsyncClient cloudWatchAsyncClient
  ) {
    io.micrometer.cloudwatch2.CloudWatchConfig cloudWatchConfig =
        new io.micrometer.cloudwatch2.CloudWatchConfig() {
          @Override
          public String get(String key) {
            return null;
          }

          @Override
          public String namespace() {
            return namespace;
          }

          @Override
          public Duration step() {
            return DurationStyle.detectAndParse(step);
          }
        };

    return new CloudWatchMeterRegistry(
        cloudWatchConfig,
        Clock.SYSTEM,
        cloudWatchAsyncClient
    );
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
      return hasText(sessionToken)
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
