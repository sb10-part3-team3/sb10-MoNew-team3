package com.team3.monew.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class BatchMetrics {

  private static final String NEWS_COLLECT_PREFIX = "monew.news.collect";
  private static final String NOTIFICATION_DELETE_PREFIX = "monew.notification.delete";
  private static final String USER_DELETE_PREFIX = "monew.user.delete";

  private final Counter newsCollectRunCounter;
  private final Counter newsCollectSuccessCounter;
  private final Counter newsCollectFailureCounter;
  private final Timer newsCollectDurationTimer;

  private final Counter notificationDeleteRunCounter;
  private final Counter notificationDeleteSuccessCounter;
  private final Counter notificationDeleteFailureCounter;
  private final Timer notificationDeleteDurationTimer;

  private final Counter userDeleteRunCounter;
  private final Counter userDeleteSuccessCounter;
  private final Counter userDeleteFailureCounter;
  private final Timer userDeleteDurationTimer;

  private final AtomicLong newsCollectSavedCount = new AtomicLong();
  private final AtomicLong newsCollectLastRunEpoch = new AtomicLong();
  private final AtomicLong newsCollectLastSuccessEpoch = new AtomicLong();

  private final AtomicLong notificationDeleteDeletedCount = new AtomicLong();
  private final AtomicLong notificationDeleteLastRunEpoch = new AtomicLong();
  private final AtomicLong notificationDeleteLastSuccessEpoch = new AtomicLong();

  private final AtomicLong userDeleteDeletedCount = new AtomicLong();
  private final AtomicLong userDeleteLastRunEpoch = new AtomicLong();
  private final AtomicLong userDeleteLastSuccessEpoch = new AtomicLong();

  public BatchMetrics(MeterRegistry meterRegistry) {
    userDeleteRunCounter = Counter.builder(USER_DELETE_PREFIX + ".run")
        .description("사용자 삭제 배치 실행 횟수")
        .register(meterRegistry);
    userDeleteSuccessCounter = Counter.builder(USER_DELETE_PREFIX + ".success")
        .description("사용자 삭제 배치 성공 횟수")
        .register(meterRegistry);
    userDeleteFailureCounter = Counter.builder(USER_DELETE_PREFIX + ".failure")
        .description("사용자 삭제 배치 실패 횟수")
        .register(meterRegistry);
    userDeleteDurationTimer = Timer.builder(USER_DELETE_PREFIX + ".duration")
        .description("사용자 삭제 배치 실행 시간")
        .register(meterRegistry);
    newsCollectRunCounter = Counter.builder(NEWS_COLLECT_PREFIX + ".run")
        .description("뉴스 수집 작업 실행 횟수")
        .register(meterRegistry);
    newsCollectSuccessCounter = Counter.builder(NEWS_COLLECT_PREFIX + ".success")
        .description("뉴스 수집 작업 성공 횟수")
        .register(meterRegistry);
    newsCollectFailureCounter = Counter.builder(NEWS_COLLECT_PREFIX + ".failure")
        .description("뉴스 수집 작업 실패 횟수")
        .register(meterRegistry);
    newsCollectDurationTimer = Timer.builder(NEWS_COLLECT_PREFIX + ".duration")
        .description("뉴스 수집 작업 실행 시간")
        .register(meterRegistry);

    notificationDeleteRunCounter = Counter.builder(NOTIFICATION_DELETE_PREFIX + ".run")
        .description("알림 삭제 배치 실행 횟수")
        .register(meterRegistry);
    notificationDeleteSuccessCounter = Counter.builder(NOTIFICATION_DELETE_PREFIX + ".success")
        .description("알림 삭제 배치 성공 횟수")
        .register(meterRegistry);
    notificationDeleteFailureCounter = Counter.builder(NOTIFICATION_DELETE_PREFIX + ".failure")
        .description("알림 삭제 배치 실패 횟수")
        .register(meterRegistry);
    notificationDeleteDurationTimer = Timer.builder(NOTIFICATION_DELETE_PREFIX + ".duration")
        .description("알림 삭제 배치 실행 시간")
        .register(meterRegistry);

    Gauge.builder(NEWS_COLLECT_PREFIX + ".saved-count", newsCollectSavedCount, AtomicLong::get)
        .description("최근 뉴스 수집 작업 저장 건수")
        .register(meterRegistry);
    Gauge.builder(NEWS_COLLECT_PREFIX + ".last-run-epoch", newsCollectLastRunEpoch, AtomicLong::get)
        .description("최근 뉴스 수집 작업 실행 시각(epoch second)")
        .register(meterRegistry);
    Gauge.builder(NEWS_COLLECT_PREFIX + ".last-success-epoch", newsCollectLastSuccessEpoch,
            AtomicLong::get)
        .description("최근 뉴스 수집 작업 성공 시각(epoch second)")
        .register(meterRegistry);

    Gauge.builder(NOTIFICATION_DELETE_PREFIX + ".deleted-count", notificationDeleteDeletedCount,
            AtomicLong::get)
        .description("최근 알림 삭제 배치 삭제 건수")
        .register(meterRegistry);
    Gauge.builder(NOTIFICATION_DELETE_PREFIX + ".last-run-epoch", notificationDeleteLastRunEpoch,
            AtomicLong::get)
        .description("최근 알림 삭제 배치 실행 시각(epoch second)")
        .register(meterRegistry);
    Gauge.builder(NOTIFICATION_DELETE_PREFIX + ".last-success-epoch",
            notificationDeleteLastSuccessEpoch, AtomicLong::get)
        .description("최근 알림 삭제 배치 성공 시각(epoch second)")
        .register(meterRegistry);
    Gauge.builder(USER_DELETE_PREFIX + ".deleted-count", userDeleteDeletedCount, AtomicLong::get)
        .description("최근 사용자 삭제 배치 삭제 건수")
        .register(meterRegistry);
    Gauge.builder(USER_DELETE_PREFIX + ".last-run-epoch", userDeleteLastRunEpoch, AtomicLong::get)
        .description("최근 사용자 삭제 배치 실행 시각(epoch second)")
        .register(meterRegistry);
    Gauge.builder(USER_DELETE_PREFIX + ".last-success-epoch", userDeleteLastSuccessEpoch,
            AtomicLong::get)
        .description("최근 사용자 삭제 배치 성공 시각(epoch second)")
        .register(meterRegistry);
  }

  public void recordNewsCollectSuccess(long durationMillis, long savedCount) {
    newsCollectRunCounter.increment();
    newsCollectSuccessCounter.increment();
    newsCollectDurationTimer.record(Duration.ofMillis(durationMillis));

    long nowEpochSecond = Instant.now().getEpochSecond();
    newsCollectSavedCount.set(savedCount);
    newsCollectLastRunEpoch.set(nowEpochSecond);
    newsCollectLastSuccessEpoch.set(nowEpochSecond);
  }

  public void recordNewsCollectFailure(long durationMillis) {
    newsCollectRunCounter.increment();
    newsCollectFailureCounter.increment();
    newsCollectDurationTimer.record(Duration.ofMillis(durationMillis));
    newsCollectLastRunEpoch.set(Instant.now().getEpochSecond());
  }

  public void recordNotificationDeleteSuccess(long durationMillis, long deletedCount) {
    notificationDeleteRunCounter.increment();
    notificationDeleteSuccessCounter.increment();
    notificationDeleteDurationTimer.record(Duration.ofMillis(durationMillis));

    long nowEpochSecond = Instant.now().getEpochSecond();
    notificationDeleteDeletedCount.set(deletedCount);
    notificationDeleteLastRunEpoch.set(nowEpochSecond);
    notificationDeleteLastSuccessEpoch.set(nowEpochSecond);
  }

  public void recordNotificationDeleteFailure(long durationMillis) {
    notificationDeleteRunCounter.increment();
    notificationDeleteFailureCounter.increment();
    notificationDeleteDurationTimer.record(Duration.ofMillis(durationMillis));
    notificationDeleteLastRunEpoch.set(Instant.now().getEpochSecond());
  }

  public void recordUserDeleteSuccess(long durationMillis, long deletedCount) {
    userDeleteRunCounter.increment();
    userDeleteSuccessCounter.increment();
    userDeleteDurationTimer.record(Duration.ofMillis(durationMillis));

    long nowEpochSecond = Instant.now().getEpochSecond();
    userDeleteDeletedCount.set(deletedCount);
    userDeleteLastRunEpoch.set(nowEpochSecond);
    userDeleteLastSuccessEpoch.set(nowEpochSecond);
  }

  public void recordUserDeleteFailure(long durationMillis) {
    userDeleteRunCounter.increment();
    userDeleteFailureCounter.increment();
    userDeleteDurationTimer.record(Duration.ofMillis(durationMillis));
    userDeleteLastRunEpoch.set(Instant.now().getEpochSecond());
  }
}
