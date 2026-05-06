package com.team3.monew.service;

import com.team3.monew.entity.ArticleBackupJob;
import com.team3.monew.entity.enums.BackupJobStatus;
import com.team3.monew.entity.enums.BackupJobType;
import com.team3.monew.exception.article.ArticleBackupJobNotFoundException;
import com.team3.monew.repository.ArticleBackupJobRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleBackupJobLogService {

  private final ArticleBackupJobRepository articleBackupJobRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public UUID createBackupJob(LocalDate localDate, String bucket, String key) {
    ArticleBackupJob backupJob = ArticleBackupJob
        .create(localDate, BackupJobType.ARTICLE_DAILY_BACKUP)
        .setS3Bucket(bucket)
        .setS3Key(key)
        .setStartedAt(Instant.now());
    articleBackupJobRepository.save(backupJob);

    UUID backupJobId = backupJob.getId();
    log.debug("BackupJob 생성 - localDate={}, id={}", localDate, backupJobId);
    return backupJobId;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordSuccess(UUID backupJobId, int articleCount) {
    ArticleBackupJob backupJob = getArticleBackupJobOrThrow(backupJobId)
        .setStatus(BackupJobStatus.SUCCESS)
        .setFinishedAt(Instant.now())
        .setArticleCount(articleCount);
    articleBackupJobRepository.save(backupJob);
    log.debug("BackupJob 성공 - localDate={}", backupJob.getBackupDate());
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordFailed(UUID backupJobId, String message) {
    ArticleBackupJob backupJob = getArticleBackupJobOrThrow(backupJobId)
        .setStatus(BackupJobStatus.FAILED)
        .setFinishedAt(Instant.now())
        .setErrorMessage(message);
    articleBackupJobRepository.save(backupJob);
    log.debug("BackupJob 실패 - localDate={}", backupJob.getBackupDate());
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Map<LocalDate, UUID> createRestoreJobAll(Set<LocalDate> localDates) {
    Map<LocalDate, UUID> restoreJobs = new HashMap<>();
    List<ArticleBackupJob> jobs = new ArrayList<>();

    localDates.forEach(localDate -> {
      ArticleBackupJob backupJob = ArticleBackupJob
          .create(localDate, BackupJobType.ARTICLE_RESTORE)
          .setStartedAt(Instant.now());
      jobs.add(backupJob);
    });
    articleBackupJobRepository.saveAll(jobs);
    jobs.forEach(job -> restoreJobs.put(job.getBackupDate(), job.getId()));

    log.debug("RestoreJob {}개 생성", jobs.size());
    return restoreJobs;
  }

  @Transactional
  public Instant recordRestoreSuccess(UUID restoreJobId, int articleCount) {
    ArticleBackupJob restoreJob = getArticleBackupJobOrThrow(restoreJobId)
        .setStatus(BackupJobStatus.SUCCESS)
        .setFinishedAt(Instant.now())
        .setArticleCount(articleCount);
    articleBackupJobRepository.save(restoreJob);
    log.debug("RestoreJob 성공 - localDate={}", restoreJob.getBackupDate());
    return restoreJob.getFinishedAt();
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordRestoreFailed(UUID restoreJobId, String message) {
    ArticleBackupJob restoreJob = getArticleBackupJobOrThrow(restoreJobId)
        .setStatus(BackupJobStatus.FAILED)
        .setFinishedAt(Instant.now())
        .setErrorMessage(message);
    articleBackupJobRepository.save(restoreJob);
    log.debug("RestoreJob 실패 - localDate={}", restoreJob.getBackupDate());
  }

  private ArticleBackupJob getArticleBackupJobOrThrow(UUID backupJobId) {
    return articleBackupJobRepository.findById(backupJobId)
        .orElseThrow(() -> new ArticleBackupJobNotFoundException(backupJobId));
  }
}
