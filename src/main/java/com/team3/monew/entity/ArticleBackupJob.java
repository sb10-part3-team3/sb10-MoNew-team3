package com.team3.monew.entity;

import com.team3.monew.entity.base.BaseEntity;
import com.team3.monew.entity.enums.BackupJobStatus;
import com.team3.monew.entity.enums.BackupJobType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "article_backup_jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleBackupJob extends BaseEntity {

  @Column(nullable = false)
  private LocalDate backupDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BackupJobType jobType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BackupJobStatus status;

  @Column(name = "s3_bucket", length = 255)
  private String s3Bucket;

  @Column(name = "s3_key", length = 500)
  private String s3Key;

  @Column(nullable = false)
  private int articleCount;

  private Instant startedAt;
  private Instant finishedAt;

  @Column(length = 1000)
  private String errorMessage;

  public static ArticleBackupJob create(
      LocalDate backupDate,
      BackupJobType jobType
  ) {
    ArticleBackupJob job = new ArticleBackupJob();
    job.backupDate = backupDate;
    job.jobType = jobType;
    job.status = BackupJobStatus.PENDING; // 초기 상태
    job.articleCount = 0;

    return job;
  }

  public ArticleBackupJob setS3Bucket(String s3bucket) {
    this.s3Bucket = s3bucket;
    return this;
  }

  public ArticleBackupJob setS3Key(String s3Key) {
    this.s3Key = s3Key;
    return this;
  }

  public ArticleBackupJob setArticleCount(int articleCount) {
    this.articleCount = articleCount;
    return this;
  }

  public ArticleBackupJob setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
    return this;
  }

  public ArticleBackupJob setStartedAt(Instant startedAt) {
    this.startedAt = startedAt;
    return this;
  }

  public ArticleBackupJob setFinishedAt(Instant finishedAt) {
    this.finishedAt = finishedAt;
    return this;
  }

  public ArticleBackupJob setStatus(BackupJobStatus status) {
    this.status = status;
    return this;
  }
}
