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

  @Modifying
  @Query(value = """
        DELETE FROM users
        WHERE id IN (
            SELECT id FROM users
            WHERE delete_status = 'DELETED'
              AND deleted_at < :targetDate
            LIMIT :batchSize
        )
        """, nativeQuery = true)
  int deleteSoftDeletedUsers(@Param("targetDate") Instant targetDate,
      @Param("batchSize") int batchSize);

  @Query("""
    SELECT u.id FROM User u
    WHERE u.deleteStatus = 'DELETED'
      AND u.deletedAt < :targetDate
""")
  List<UUID> findDeletableUserIds(Instant targetDate, Pageable pageable);
}
