package com.team3.monew.controller;

import com.team3.monew.controller.api.InterestApi;
import com.team3.monew.dto.interest.InterestDto;
import com.team3.monew.dto.interest.InterestRegisterRequest;
import com.team3.monew.dto.interest.InterestUpdateRequest;
import com.team3.monew.dto.interest.SubscriptionDto;
import com.team3.monew.dto.interest.internal.InterestCursor;
import com.team3.monew.dto.interest.internal.InterestSearchCondition;
import com.team3.monew.dto.pagination.CursorPageResponseDto;
import com.team3.monew.service.InterestService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InterestController implements InterestApi {

  private final InterestService interestService;

  @Override
  public ResponseEntity<InterestDto> create(
      @Valid @RequestBody InterestRegisterRequest dto
  ) {
    InterestDto response = interestService.createInterest(dto);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(response);
  }

  @Override
  public ResponseEntity<InterestDto> update(
      UUID interestId,
      @Valid @RequestBody InterestUpdateRequest dto
  ) {
    InterestDto response = interestService.updateKeyword(interestId, dto);

    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<Void> delete(UUID interestId) {
    interestService.deleteInterest(interestId);

    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<CursorPageResponseDto<InterestDto>> findAll(
      UUID userId,
      String keyword,
      String orderBy,
      String direction,
      String cursor,
      Instant after,
      int limit
  ) {
    InterestSearchCondition condition = new InterestSearchCondition(
        keyword,
        orderBy,
        direction,
        new InterestCursor(cursor, after),
        limit
    );

    CursorPageResponseDto<InterestDto> response =
        interestService.findAll(condition, userId);

    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<SubscriptionDto> subscribe(UUID userId, UUID interestId) {
    SubscriptionDto response = interestService.subscribe(userId, interestId);

    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<Void> cancelSubscribe(UUID userId, UUID interestId) {
    interestService.cancelSubscribe(userId, interestId);

    return ResponseEntity.noContent().build();
  }
}
