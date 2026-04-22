package com.team3.monew.repository;

import com.team3.monew.entity.Interest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InterestRepository extends JpaRepository<Interest, UUID>,
    InterestRepositoryCustom {

  boolean existsByName(String name);
}
