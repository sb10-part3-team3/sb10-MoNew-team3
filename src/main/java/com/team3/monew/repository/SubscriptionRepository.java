package com.team3.monew.repository;

import com.team3.monew.entity.Subscription;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

  boolean existsByUserIdAndInterestId(UUID userId, UUID interestId);
}
