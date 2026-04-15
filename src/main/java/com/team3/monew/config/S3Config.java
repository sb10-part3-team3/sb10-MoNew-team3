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
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AwsProperties.class)
public class S3Config {

  private final AwsProperties props;

  @Bean
  public S3Client s3Client() {
    return S3Client.builder()
        .region(Region.of(props.getRegion()))
        .credentialsProvider(getCredentialsProvider())
        .build();
  }

  private AwsCredentialsProvider getCredentialsProvider() {
    String accessKey = props.getCredentials().getAccessKey();
    String secretKey = props.getCredentials().getSecretKey();

    return accessKey != null && !accessKey.isBlank()
        && secretKey != null && !secretKey.isBlank()
        // 수동탐색 방식: (.env, yaml 설정파일 사용)
        ? StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
        // 자동탐색 방식: (Java 시스템 속성 -> 환경 변수 -> 자격 증명 파일(AWS CLI 설정값) -> 컨테이너/EC2(IAM ROLE))
        : DefaultCredentialsProvider.create();
  }
}
