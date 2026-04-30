package com.team3.monew.repository;

import com.team3.monew.entity.NewsSource;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsSourceRepository extends JpaRepository<NewsSource, UUID> {

  Optional<NewsSource> findByName(String name);
}
