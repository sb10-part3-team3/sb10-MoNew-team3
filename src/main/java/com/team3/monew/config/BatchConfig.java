package com.team3.monew.config;

import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Isolation;

@Configuration
public class BatchConfig extends DefaultBatchConfiguration {

  // PostgreSQL의 SERIALIZABLE 격리 수준에서 발생하는 동시 실행 트랜잭션 충돌방지
  // 격리 수준을 READ_COMMITTED로 조정 (메타데이터 저장 시의 데드락 방지)
  @Override
  protected Isolation getIsolationLevelForCreate() {
    return Isolation.READ_COMMITTED;
  }
}
