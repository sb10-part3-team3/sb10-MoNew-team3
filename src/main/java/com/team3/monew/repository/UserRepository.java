package com.team3.monew.repository;

import com.team3.monew.entity.User;
import com.team3.monew.entity.enums.DeleteStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  boolean existsByIdAndDeleteStatus(UUID id, DeleteStatus deleteStatus);

  Optional<User> findByIdAndDeleteStatus(UUID id, DeleteStatus deleteStatus);
}
