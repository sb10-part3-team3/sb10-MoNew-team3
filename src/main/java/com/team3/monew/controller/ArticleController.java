package com.team3.monew.controller;

import com.team3.monew.controller.api.ArticleApi;
import com.team3.monew.dto.article.ArticleViewDto;
import com.team3.monew.service.ArticleService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/articles")
public class ArticleController implements ArticleApi {

  private final ArticleService articleService;

  @PostMapping("/{articleId}/article-views")
  public ResponseEntity<ArticleViewDto> registerArticleView(
      @PathVariable("articleId") UUID articleId,
      @RequestHeader(REQUEST_USER_ID_HEADER) UUID requestUserId
  ) {
    return ResponseEntity.ok(articleService.registerArticleView(articleId, requestUserId));
  }
}
