package com.team3.monew.repository;

import com.team3.monew.entity.InterestKeyword;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InterestKeywordRepository extends JpaRepository<InterestKeyword, UUID> {

  List<InterestKeyword> findAllByInterestId(UUID interestId);

  @Query("SELECT ik FROM InterestKeyword ik JOIN FETCH ik.interest")
  List<InterestKeyword> findAllWithInterest();
}
