package com.team3.monew.controller;

import com.team3.monew.controller.api.ArticleSourceApi;
import com.team3.monew.service.ArticleSourceService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleSourceController implements ArticleSourceApi {

  private final ArticleSourceService articleSourceService;

  @Override
  public ResponseEntity<List<String>> getArticleSources() {
    return ResponseEntity.status(HttpStatus.OK)
        .body(articleSourceService.getArticleSources());
  }
}
