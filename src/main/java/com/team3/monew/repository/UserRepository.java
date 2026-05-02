package com.team3.monew.repository;

import com.team3.monew.entity.User;
import com.team3.monew.entity.enums.DeleteStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  boolean existsByIdAndDeleteStatus(UUID id, DeleteStatus deleteStatus);

  Optional<User> findByIdAndDeleteStatus(UUID id, DeleteStatus deleteStatus);

  @Query("""
    SELECT u.id FROM User u
    WHERE u.deleteStatus = com.team3.monew.entity.enums.DeleteStatus.DELETED
      AND u.deletedAt < :targetDate
""")
  List<UUID> findDeletableUserIds(@Param("targetDate") Instant targetDate, Pageable pageable);

  @Modifying
  @Query("DELETE FROM User u WHERE u.id IN :userIds")
  int deleteByIds(@Param("userIds") List<UUID> userIds);
}
