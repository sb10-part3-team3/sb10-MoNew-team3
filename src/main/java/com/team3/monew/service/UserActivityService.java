package com.team3.monew.service;

import com.team3.monew.document.UserActivityDocument;
import com.team3.monew.document.UserActivityRequest;
import com.team3.monew.dto.useractivity.UserActivityDto;
import com.team3.monew.exception.useractivity.UserActivityNotFoundException;
import com.team3.monew.mapper.UserActivityMapper;
import com.team3.monew.repository.UserActivityRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserActivityService {

  private final UserActivityRepository userActivityRepository;
  private final UserActivityMapper userActivityMapper;

  public UserActivityDto findUserActivity(UUID userId) {
    UserActivityDocument userActivityDocument = userActivityRepository.findById(userId)
        .orElseThrow(() -> new UserActivityNotFoundException(userId));
    return userActivityMapper.toDto(userActivityDocument);
  }

  public void registerUserActivity(UserActivityRequest userActivityRequest) {
    userActivityRepository.save(userActivityMapper.toDocument(userActivityRequest));
  }
}
