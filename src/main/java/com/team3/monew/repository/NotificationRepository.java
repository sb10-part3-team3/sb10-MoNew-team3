package com.team3.monew.repository;

import com.team3.monew.entity.Notification;
import com.team3.monew.entity.enums.NotificationResourceType;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  void deleteByResourceTypeAndResourceId(NotificationResourceType resourceType, UUID resourceId);
}
