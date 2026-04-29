package com.team3.monew.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "api.naver.credentials")
public class NaverProperties {

  private final String id;
  private final String secret;

}
