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
  private static final String ARTICLE_BACKUP_PREFIX = "monew.article.backup";

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

  private final Counter articleBackupRunCounter;
  private final Counter articleBackupSuccessCounter;
  private final Counter articleBackupFailureCounter;
  private final Timer articleBackupDurationTimer;

  private final AtomicLong newsCollectSavedCount = new AtomicLong();
  private final AtomicLong newsCollectLastRunEpoch = new AtomicLong();
  private final AtomicLong newsCollectLastSuccessEpoch = new AtomicLong();

  private final AtomicLong notificationDeleteDeletedCount = new AtomicLong();
  private final AtomicLong notificationDeleteLastRunEpoch = new AtomicLong();
  private final AtomicLong notificationDeleteLastSuccessEpoch = new AtomicLong();

  private final AtomicLong userDeleteDeletedCount = new AtomicLong();
  private final AtomicLong userDeleteLastRunEpoch = new AtomicLong();
  private final AtomicLong userDeleteLastSuccessEpoch = new AtomicLong();

  private final AtomicLong articleBackupArticleCount = new AtomicLong();
  private final AtomicLong articleBackupLastRunEpoch = new AtomicLong();
  private final AtomicLong articleBackupLastSuccessEpoch = new AtomicLong();

  public BatchMetrics(MeterRegistry meterRegistry) {
    newsCollectRunCounter = Counter.builder(NEWS_COLLECT_PREFIX + ".run")
        .description("News collect batch run count")
        .register(meterRegistry);
    newsCollectSuccessCounter = Counter.builder(NEWS_COLLECT_PREFIX + ".success")
        .description("News collect batch success count")
        .register(meterRegistry);
    newsCollectFailureCounter = Counter.builder(NEWS_COLLECT_PREFIX + ".failure")
        .description("News collect batch failure count")
        .register(meterRegistry);
    newsCollectDurationTimer = Timer.builder(NEWS_COLLECT_PREFIX + ".duration")
        .description("News collect batch duration")
        .register(meterRegistry);

    notificationDeleteRunCounter = Counter.builder(NOTIFICATION_DELETE_PREFIX + ".run")
        .description("Notification delete batch run count")
        .register(meterRegistry);
    notificationDeleteSuccessCounter = Counter.builder(NOTIFICATION_DELETE_PREFIX + ".success")
        .description("Notification delete batch success count")
        .register(meterRegistry);
    notificationDeleteFailureCounter = Counter.builder(NOTIFICATION_DELETE_PREFIX + ".failure")
        .description("Notification delete batch failure count")
        .register(meterRegistry);
    notificationDeleteDurationTimer = Timer.builder(NOTIFICATION_DELETE_PREFIX + ".duration")
        .description("Notification delete batch duration")
        .register(meterRegistry);

    userDeleteRunCounter = Counter.builder(USER_DELETE_PREFIX + ".run")
        .description("User delete batch run count")
        .register(meterRegistry);
    userDeleteSuccessCounter = Counter.builder(USER_DELETE_PREFIX + ".success")
        .description("User delete batch success count")
        .register(meterRegistry);
    userDeleteFailureCounter = Counter.builder(USER_DELETE_PREFIX + ".failure")
        .description("User delete batch failure count")
        .register(meterRegistry);
    userDeleteDurationTimer = Timer.builder(USER_DELETE_PREFIX + ".duration")
        .description("User delete batch duration")
        .register(meterRegistry);

    articleBackupRunCounter = Counter.builder(ARTICLE_BACKUP_PREFIX + ".run")
        .description("Article backup batch run count")
        .register(meterRegistry);
    articleBackupSuccessCounter = Counter.builder(ARTICLE_BACKUP_PREFIX + ".success")
        .description("Article backup batch success count")
        .register(meterRegistry);
    articleBackupFailureCounter = Counter.builder(ARTICLE_BACKUP_PREFIX + ".failure")
        .description("Article backup batch failure count")
        .register(meterRegistry);
    articleBackupDurationTimer = Timer.builder(ARTICLE_BACKUP_PREFIX + ".duration")
        .description("Article backup batch duration")
        .register(meterRegistry);

    Gauge.builder(NEWS_COLLECT_PREFIX + ".saved-count", newsCollectSavedCount, AtomicLong::get)
        .description("Latest saved article count")
        .register(meterRegistry);
    Gauge.builder(NEWS_COLLECT_PREFIX + ".last-run-epoch", newsCollectLastRunEpoch, AtomicLong::get)
        .description("Latest news collect batch run time in epoch seconds")
        .register(meterRegistry);
    Gauge.builder(NEWS_COLLECT_PREFIX + ".last-success-epoch", newsCollectLastSuccessEpoch,
            AtomicLong::get)
        .description("Latest news collect batch success time in epoch seconds")
        .register(meterRegistry);

    Gauge.builder(NOTIFICATION_DELETE_PREFIX + ".deleted-count", notificationDeleteDeletedCount,
            AtomicLong::get)
        .description("Latest deleted notification count")
        .register(meterRegistry);
    Gauge.builder(NOTIFICATION_DELETE_PREFIX + ".last-run-epoch", notificationDeleteLastRunEpoch,
            AtomicLong::get)
        .description("Latest notification delete batch run time in epoch seconds")
        .register(meterRegistry);
    Gauge.builder(NOTIFICATION_DELETE_PREFIX + ".last-success-epoch",
            notificationDeleteLastSuccessEpoch, AtomicLong::get)
        .description("Latest notification delete batch success time in epoch seconds")
        .register(meterRegistry);

    Gauge.builder(USER_DELETE_PREFIX + ".deleted-count", userDeleteDeletedCount, AtomicLong::get)
        .description("Latest deleted user count")
        .register(meterRegistry);
    Gauge.builder(USER_DELETE_PREFIX + ".last-run-epoch", userDeleteLastRunEpoch, AtomicLong::get)
        .description("Latest user delete batch run time in epoch seconds")
        .register(meterRegistry);
    Gauge.builder(USER_DELETE_PREFIX + ".last-success-epoch", userDeleteLastSuccessEpoch,
            AtomicLong::get)
        .description("Latest user delete batch success time in epoch seconds")
        .register(meterRegistry);

    Gauge.builder(ARTICLE_BACKUP_PREFIX + ".article-count", articleBackupArticleCount,
            AtomicLong::get)
        .description("Latest backed up article count")
        .register(meterRegistry);
    Gauge.builder(ARTICLE_BACKUP_PREFIX + ".last-run-epoch", articleBackupLastRunEpoch,
            AtomicLong::get)
        .description("Latest article backup batch run time in epoch seconds")
        .register(meterRegistry);
    Gauge.builder(ARTICLE_BACKUP_PREFIX + ".last-success-epoch", articleBackupLastSuccessEpoch,
            AtomicLong::get)
        .description("Latest article backup batch success time in epoch seconds")
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

  public void recordArticleBackupSuccess(long durationMillis, long articleCount) {
    articleBackupRunCounter.increment();
    articleBackupSuccessCounter.increment();
    articleBackupDurationTimer.record(Duration.ofMillis(durationMillis));

    long nowEpochSecond = Instant.now().getEpochSecond();
    articleBackupArticleCount.set(articleCount);
    articleBackupLastRunEpoch.set(nowEpochSecond);
    articleBackupLastSuccessEpoch.set(nowEpochSecond);
  }

  public void recordArticleBackupFailure(long durationMillis) {
    articleBackupRunCounter.increment();
    articleBackupFailureCounter.increment();
    articleBackupDurationTimer.record(Duration.ofMillis(durationMillis));
    articleBackupLastRunEpoch.set(Instant.now().getEpochSecond());
  }
}
