package com.team3.monew.repository;

import com.team3.monew.entity.ArticleInterest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleInterestRepository extends JpaRepository<ArticleInterest, UUID> {

  List<ArticleInterest> findAllByInterestId(UUID interestId);

}
