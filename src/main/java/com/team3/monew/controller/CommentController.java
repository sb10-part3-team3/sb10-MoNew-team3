package com.team3.monew.controller;

import com.team3.monew.controller.api.CommentApi;
import com.team3.monew.dto.comment.CommentDto;
import com.team3.monew.dto.comment.CommentRegisterRequest;
import com.team3.monew.global.enums.ErrorCode;
import com.team3.monew.global.exception.BusinessException;
import com.team3.monew.service.CommentService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentController implements CommentApi {

  private final CommentService commentService;

  @PostMapping
  public ResponseEntity<CommentDto> registerComment(
      @Parameter(hidden = true) Principal principal,
      @Valid @RequestBody CommentRegisterRequest commentRegisterRequest
  ) {
    UUID userId = extractUserId(principal);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(commentService.registerComment(commentRegisterRequest, userId));
  }

  private UUID extractUserId(Principal principal) {
    if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, Map.of("principal", "required"));
    }

    try {
      return UUID.fromString(principal.getName());
    } catch (IllegalArgumentException e) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, Map.of("principal", "invalid"));
    }
  }
}
