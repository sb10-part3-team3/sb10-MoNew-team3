package com.team3.monew.config;

import java.time.Duration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

  private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
      "postgres:15-alpine")
      .withReuse(true)
      .withStartupTimeout(Duration.ofSeconds(120));

  // 컨테이너 한 개만
  static {
    postgres.start();
  }

  @Bean
  @ServiceConnection
  public PostgreSQLContainer<?> postgresContainer() {
    return postgres;
  }
}