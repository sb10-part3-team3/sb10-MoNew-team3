package com.team3.monew.entity;

import com.team3.monew.entity.base.BaseEntity;
import com.team3.monew.entity.enums.BackupJobStatus;
import com.team3.monew.entity.enums.BackupJobType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

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
}
