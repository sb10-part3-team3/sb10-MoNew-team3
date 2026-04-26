package com.team3.monew.controller;

import com.team3.monew.controller.api.ArticleApi;
import com.team3.monew.dto.article.ArticleDto;
import com.team3.monew.dto.article.ArticleSearchRequest;
import com.team3.monew.dto.pagination.CursorPageResponseDto;
import com.team3.monew.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController implements ArticleApi {

  private final ArticleService articleService;

  @GetMapping
  public ResponseEntity<CursorPageResponseDto<ArticleDto>> getArticleList(
      @ParameterObject @Valid ArticleSearchRequest searchRequest,
      @RequestHeader(REQUEST_USER_ID_HEADER) String requestUserId
  ) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(articleService.getArticleList(searchRequest));
  }
}
