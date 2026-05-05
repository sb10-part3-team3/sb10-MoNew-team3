package com.team3.monew.testdata;

import com.team3.monew.entity.User;
import com.team3.monew.testdata.generator.NotificationGenerator;
import com.team3.monew.testdata.generator.UserGenerator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("data-gen")
@Order(2)//datasource 저장 이후 수행
@RequiredArgsConstructor
public class DataGenerationRunner implements CommandLineRunner {

  private final ApplicationContext context;
  private final UserGenerator userGenerator;
  private final NotificationGenerator notificationGenerator;

  // private final로 제너레이트 추가

  @Override
  public void run(String... args) throws Exception {
    log.info("데이터 생성 시작");

    // 데이터 생성 순서 주의(의존 관계 고려)
    List<User> generatedUsers = userGenerator.generate(10000, 1000); // 1만 명 생성, 1천 건씩 배치
    log.info("✅ 사용자 데이터 생성 완료");

    notificationGenerator.setUsers(generatedUsers);
    notificationGenerator.generate(10000, 1000);
    log.info("✅ 알림 데이터 생성 완료");

    // 생성 후 종료
    SpringApplication.exit(context, () -> 0);
  }
}
