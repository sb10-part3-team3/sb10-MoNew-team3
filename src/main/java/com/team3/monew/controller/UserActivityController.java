package com.team3.monew.controller;

import com.team3.monew.controller.api.UserActivityApi;
import com.team3.monew.dto.useractivity.UserActivityDto;
import com.team3.monew.service.UserActivityService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user-activities")
public class UserActivityController implements UserActivityApi {

  private final UserActivityService userActivityService;

  @GetMapping("/{userId}")
  public ResponseEntity<UserActivityDto> findUserActivity(@PathVariable UUID userId) {
    return ResponseEntity.ok(userActivityService.findUserActivity(userId));
  }
}
