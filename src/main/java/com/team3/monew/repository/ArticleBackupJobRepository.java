package com.team3.monew.repository;

import com.team3.monew.entity.ArticleBackupJob;
import com.team3.monew.entity.enums.BackupJobStatus;
import com.team3.monew.entity.enums.BackupJobType;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleBackupJobRepository extends JpaRepository<ArticleBackupJob, UUID> {

  List<ArticleBackupJob> findAllByBackupDateBetweenAndJobTypeAndStatus(LocalDate backupDateAfter,
      LocalDate backupDateBefore, BackupJobType jobType, BackupJobStatus status);
}
