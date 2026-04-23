package com.team3.monew.config;

import java.util.Arrays;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@EnableRetry
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

  //좋아요 알림처럼 실시간으로 이벤트가 발행되는 스레드풀
  @Bean(name = "realTimeNotificationTaskExecutor")
  public TaskExecutor createRealTimeNotificationTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(3); //기본 스레드 수
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(50);//스레드 별 대기 가능
    executor.setThreadNamePrefix("real-time-noti-task-");//스레드 이름
    executor.setWaitForTasksToCompleteOnShutdown(true); // 진행 중인 작업 완료 후 종료
    executor.setAwaitTerminationSeconds(30);           // 최대 대기 시간 설정
    executor.initialize();
    return executor;
  }

  //배치 이벤트가 발행되는 스레드풀
  @Bean(name = "batchNotificationTaskExecutor")
  public TaskExecutor createBatchNotificationTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5); //기본 스레드 수
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(100);//스레드 별 대기 가능
    executor.setThreadNamePrefix("batch-noti-task-");//스레드 이름
    executor.setWaitForTasksToCompleteOnShutdown(true); // 진행 중인 작업 완료 후 종료
    executor.setAwaitTerminationSeconds(100);           // 최대 대기 시간 설정
    executor.setRejectedExecutionHandler(
        new ThreadPoolExecutor.CallerRunsPolicy());//큐가 가득찾을 때 메인 스레드 사용
    executor.initialize();
    return executor;
  }

  // 사용자 활동 내역 실시간 갱신용 (뉴스 기사 배치의 영향 없으므로 배치용 x)
  @Bean(name = "userActivityTaskExecutor")
  public TaskExecutor userActivityTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    executor.setCorePoolSize(2); // 평소에는 2개 정도 작업자가 상시 대기
    executor.setMaxPoolSize(8); // 순간 트래픽이 몰릴 때 최대 8개까지 확장
    executor.setQueueCapacity(100);// 너무 큰 큐로 문제를 숨기지 않도록 중간 수준으로 제한
    executor.setThreadNamePrefix("user-activity-");

    executor.setWaitForTasksToCompleteOnShutdown(true); // 종료 시 진행 중인 작업은 최대한 마무리
    executor.setAwaitTerminationSeconds(30);

    // 활동 내역은 부가 기능이므로 기본 거절 정책 유지(메인 요청 우선, 실패 후 로그 남기기 위해)

    executor.initialize();
    return executor;
  }

  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return (ex, method, params) -> {
      log.error("비동기 처리 중 예외 발생 - 메서드: {}, 파라미터: {}",
          method.getName(),
          extractParamTypes(params),
          ex);
      //추후 알림 추가 가능
    };
  }

  private String extractParamTypes(Object[] params) {
    if (params == null || params.length == 0) {
      return "[]";
    }
    return Arrays.stream(params)
        .map(p -> p == null ? "null" : p.getClass().getSimpleName())
        .toList()
        .toString();
  }
}
