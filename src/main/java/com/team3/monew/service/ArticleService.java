package com.team3.monew.service;

import com.team3.monew.dto.article.ArticleViewDto;
import com.team3.monew.mapper.ArticleMapper;
import com.team3.monew.repository.ArticleViewRepository;
import com.team3.monew.repository.NewsArticleRepository;
import com.team3.monew.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArticleService {

  private final NewsArticleRepository newsArticleRepository;
  private final UserRepository userRepository;
  private final ArticleViewRepository articleViewRepository;
  private final ArticleMapper articleMapper;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public ArticleViewDto registerArticleView(UUID articleId, UUID requestUserId) {
    throw new UnsupportedOperationException("registerArticleView is not implemented yet");
  }

  @Transactional(readOnly = true)
  public List<String> getSources() {
    throw new UnsupportedOperationException("getSources is not implemented yet");
  }
}
