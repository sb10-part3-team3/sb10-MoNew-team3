package com.team3.monew.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "aws")
public class AwsProperties {

  private Credentials credentials;
  private String region;
  private S3 s3 = new S3();

  @Getter
  @Setter
  public static class Credentials {

    private String accessKey;
    private String secretKey;
  }

  @Getter
  @Setter
  public static class S3 {

    private String bucket;
  }
}
