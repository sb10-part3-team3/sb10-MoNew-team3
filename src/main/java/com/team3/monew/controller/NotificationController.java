package com.team3.monew.controller;

import com.team3.monew.controller.api.NotificationApi;
import com.team3.monew.dto.notification.NotificationDto;
import com.team3.monew.dto.pagination.CursorPageResponseDto;
import com.team3.monew.service.NotificationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/notifications")
public class NotificationController implements NotificationApi {

  private final NotificationService notificationService;

  @GetMapping
  public ResponseEntity<CursorPageResponseDto<NotificationDto>> findAllNotConfirmed(
      @RequestHeader(REQUEST_USER_ID_HEADER) UUID requestUserId,
      @RequestParam(required = false) UUID cursor,
      @RequestParam(required = false) Instant after,
      @RequestParam(defaultValue = "50") @Min(1) @Max(100) Integer limit
  ) {
    CursorPageResponseDto<NotificationDto> dto = notificationService.findAllNotConfirmed(
        requestUserId,
        cursor, after, limit);
    return ResponseEntity.ok(dto);
  }

  @PatchMapping("/{notificationId}")
  public ResponseEntity<?> confirm(
      @RequestHeader(REQUEST_USER_ID_HEADER) UUID requestUserId,
      @PathVariable UUID notificationId) {
    notificationService.confirm(requestUserId, notificationId);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping()
  public ResponseEntity<?> confirmAll(
      @RequestHeader(REQUEST_USER_ID_HEADER) UUID requestUserId) {
    notificationService.confirmAll(requestUserId);
    return ResponseEntity.noContent().build();
  }
}
