package com.team3.monew.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

  @Bean
  @Primary    // 우선순위
  public ObjectMapper createObjectMapper() {    // json Parsing용
    return new ObjectMapper()
        .registerModule(new JavaTimeModule());
  }

  @Bean
  public XmlMapper createXmlMapper() {          //  xml Parsing용
    return XmlMapper.builder()
        .addModule(new JavaTimeModule())
        .build();
  }
}
