package com.team3.monew.repository;

import com.team3.monew.entity.Notification;
import com.team3.monew.entity.enums.NotificationResourceType;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  void deleteByResourceTypeAndResourceId(NotificationResourceType resourceType, UUID resourceId);

  @Query("select n from Notification n "
      + "where n.user.id = :userId "
      + "and n.isConfirmed = false "
      + "and ("
      + "  cast(:after as timestamp) is null or"
      + "  n.createdAt < :after or " // 과거
      + "  (n.createdAt = :after and (cast(:cursor as uuid) is null or n.id > :cursor))"
      // 시간은 같은 경우
      + ") ")
  Page<Notification> findAllNotConfirmedNotificationByUserId(UUID userId, UUID cursor,
      Instant after, Pageable pageable);

  Long countByUserIdAndIsConfirmedFalse(UUID userId);
}
