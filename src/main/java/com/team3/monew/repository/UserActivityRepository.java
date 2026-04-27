package com.team3.monew.repository;

import com.team3.monew.document.UserActivityDocument;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserActivityRepository extends MongoRepository<UserActivityDocument, UUID> {

}
