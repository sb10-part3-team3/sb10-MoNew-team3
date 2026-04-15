package com.team3.monew.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

  @Bean
  public OpenAPI monewOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("Monew API 정의")
            .description("Monew 서비스 시스템 API 입니다")
            .version("v1.0"));
  }
}
