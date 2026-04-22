package com.team3.monew.controller;

import com.team3.monew.controller.api.NotificationApi;
import com.team3.monew.dto.notification.CursorPageResponseNotificationDto;
import com.team3.monew.service.NotificationService;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController implements NotificationApi {

  private final NotificationService notificationService;

  @GetMapping
  public ResponseEntity<CursorPageResponseNotificationDto> findAllNotConfirmed(
      @RequestHeader("Monew-Request-User-ID") UUID requestUserId,
      @RequestParam(required = false) Instant cursor,
      @RequestParam(required = false) UUID after,
      @RequestParam(defaultValue = "50") Integer limit
  ) {
    CursorPageResponseNotificationDto dto = notificationService.findAllNotConfirmed(requestUserId,
        cursor, after, limit);
    return ResponseEntity.ok(dto);
  }
}
