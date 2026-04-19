package com.team3.monew.controller;

import com.team3.monew.controller.api.InterestApi;
import com.team3.monew.dto.interest.InterestDto;
import com.team3.monew.dto.interest.InterestRegisterRequest;
import com.team3.monew.service.InterestService;
import jakarta.validation.Valid;
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
    InterestDto response = interestService.create(dto);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(response);
  }
}
