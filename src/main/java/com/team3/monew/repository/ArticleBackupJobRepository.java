package com.team3.monew.repository;

import com.team3.monew.entity.ArticleBackupJob;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleBackupJobRepository extends JpaRepository<ArticleBackupJob, UUID> {

}
