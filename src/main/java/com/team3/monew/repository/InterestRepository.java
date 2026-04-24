package com.team3.monew.repository;

import com.team3.monew.entity.Interest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InterestRepository extends JpaRepository<Interest, UUID>,
    InterestRepositoryCustom {

  boolean existsByName(String name);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("update Interest i set i.subscriberCount = i.subscriberCount + 1 where i.id = :interestId")
  void increaseSubscriberCount(@Param("interestId") UUID interestId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update Interest i
      set i.subscriberCount = i.subscriberCount - 1
      where i.id = :interestId
        and i.subscriberCount > 0
      """)
  int decreaseSubscriberCount(@Param("interestId") UUID interestId);
}
