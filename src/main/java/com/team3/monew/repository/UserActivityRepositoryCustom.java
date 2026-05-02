package com.team3.monew.repository;

import java.util.List;
import java.util.UUID;

public interface UserActivityRepositoryCustom {

  void removeEmbeddedCommentsByUserIds(List<UUID> userIds);
}
