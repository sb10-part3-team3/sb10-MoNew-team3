package com.team3.monew.config;

import java.util.Arrays;
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
