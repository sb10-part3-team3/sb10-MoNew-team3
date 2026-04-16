package com.team3.monew.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    // BCrypt 방식으로 암호화
    return new BCryptPasswordEncoder();
  }

  // PasswordEncoder 기능만 사용하기 위해 Spring Security 기본 보안 설정 비활성화
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // CSRF 보호 비활성화
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(
            auth -> auth
                // 모든 요청 허용
                .anyRequest().permitAll()
        );

    return http.build();
  }
}
