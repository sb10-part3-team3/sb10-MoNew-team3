package com.team3.monew.service;

import com.team3.monew.entity.ArticleBackupJob;
import com.team3.monew.entity.enums.BackupJobStatus;
import com.team3.monew.entity.enums.BackupJobType;
import com.team3.monew.repository.ArticleBackupJobRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

    return backupJob.getId();
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordSuccess(UUID backupJobId, int articleCount) {
    ArticleBackupJob backupJob = getArticleBackupJobOrThrow(backupJobId)
        .setStatus(BackupJobStatus.SUCCESS)
        .setFinishedAt(Instant.now())
        .setArticleCount(articleCount);
    articleBackupJobRepository.save(backupJob);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordFailed(UUID backupJobId, String message) {
    ArticleBackupJob backupJob = getArticleBackupJobOrThrow(backupJobId)
        .setStatus(BackupJobStatus.FAILED)
        .setFinishedAt(Instant.now())
        .setErrorMessage(message);
    articleBackupJobRepository.save(backupJob);
  }

  private ArticleBackupJob getArticleBackupJobOrThrow(UUID backupJobId) {
    return articleBackupJobRepository.findById(backupJobId)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 backupJobId입니다: " + backupJobId));
  }
}
