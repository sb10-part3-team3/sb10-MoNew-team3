package com.team3.monew.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "cloud.aws")
public class AwsProperties {

  private final Credentials credentials;
  private final Region region;
  private final S3 s3;

  @Getter
  @Setter
  public static class Credentials {

    private String accessKey;
    private String secretKey;
  }

  @Getter
  @Setter
  public static class Region {

    private String staticRegion;

    // Yaml의 static과 매핑용
    public void setStatic(String staticRegion) {
      this.staticRegion = staticRegion;
    }
  }

  @Getter
  @Setter
  public static class S3 {

    private String bucket;
  }
}
