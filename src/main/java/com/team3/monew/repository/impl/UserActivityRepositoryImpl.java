package com.team3.monew.repository.impl;

import com.team3.monew.repository.UserActivityRepositoryCustom;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserActivityRepositoryImpl implements UserActivityRepositoryCustom {

  private final MongoTemplate mongoTemplate;

  @Override
  public void removeEmbeddedCommentsByUserIds(List<UUID> userIds) {
    Query query = new Query(
        Criteria.where("commentLikes.commentUserId").in(userIds)
    );

    Update update = new Update().pull(
        "commentLikes",
        Query.query(Criteria.where("commentUserId").in(userIds))
    );

    mongoTemplate.updateMulti(query, update, "user_activities");
  }
}
