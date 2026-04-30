package com.team3.monew.repository;

import com.team3.monew.entity.Subscription;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

  boolean existsByUserIdAndInterestId(UUID userId, UUID interestId);

  Optional<Subscription> findByUserIdAndInterestId(UUID userId, UUID interestId);

  List<Subscription> findAllByInterestId(UUID interestId);

  public interface SubscriptionInfo {

    UUID getUserId();

    UUID getInterestId();
  }

  @Query("select s.user.id as userId, s.interest.id as interestId from Subscription s where s.interest.id in :interestIds")
  List<SubscriptionInfo> findAllProjectedByInterestIdIn(Collection<UUID> interestIds);

  @Query("""
          select s.interest.id
          from Subscription s
          where s.user.id = :userId
            and s.interest.id in :interestIds
      """)
  Set<UUID> findSubscribedInterestIds(
      @Param("userId") UUID userId,
      @Param("interestIds") List<UUID> interestIds
  );

  void deleteAllByUserId(@Param("userId") UUID userId);

  @Modifying
  @Query("DELETE FROM Subscription s WHERE s.user.id IN :userIds")
  void deleteByUserIds(@Param("userIds") List<UUID> userIds);
}
