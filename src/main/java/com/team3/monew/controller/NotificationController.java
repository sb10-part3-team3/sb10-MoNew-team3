package com.team3.monew.controller;

import com.team3.monew.controller.api.NotificationApi;
import com.team3.monew.dto.notification.NotificationDto;
import com.team3.monew.dto.pagination.CursorPageResponseDto;
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
  public ResponseEntity<CursorPageResponseDto<NotificationDto>> findAllNotConfirmed(
      @RequestHeader("Monew-Request-User-ID") UUID requestUserId,
      @RequestParam(required = false) UUID cursor,
      @RequestParam(required = false) Instant after,
      @RequestParam(defaultValue = "50") Integer limit
  ) {
    CursorPageResponseDto<NotificationDto> dto = notificationService.findAllNotConfirmed(
        requestUserId,
        cursor, after, limit);
    return ResponseEntity.ok(dto);
  }
}
