package com.team3.monew.service;

import com.team3.monew.dto.article.ArticleViewDto;
import com.team3.monew.entity.ArticleView;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.User;
import com.team3.monew.event.ArticleViewEvent;
import com.team3.monew.exception.article.ArticleNotFoundException;
import com.team3.monew.exception.article.DeletedArticleException;
import com.team3.monew.exception.user.DeletedUserException;
import com.team3.monew.exception.user.UserNotFoundException;
import com.team3.monew.mapper.ArticleViewMapper;
import com.team3.monew.repository.ArticleViewRepository;
import com.team3.monew.repository.NewsArticleRepository;
import com.team3.monew.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleViewService {

  private final NewsArticleRepository newsArticleRepository;
  private final UserRepository userRepository;
  private final ArticleViewRepository articleViewRepository;
  private final ArticleViewMapper articleViewMapper;
  private final ApplicationEventPublisher eventPublisher;
  private final EntityManager entityManager;

  @Transactional
  public ArticleViewDto registerArticleView(UUID articleId, UUID requestUserId) {
    log.debug("기사 뷰 등록 요청 - articleId={}, requestUserId={}", articleId, requestUserId);
    NewsArticle article = findActiveArticle(articleId);
    User user = findActiveUser(requestUserId);

    // 재조회 시 이벤트 미발행 하기 위한 boolean
    AtomicBoolean isFirstView = new AtomicBoolean(false);

    // 같은 사용자의 재조회는 기존 이력을 갱신하고, 첫 조회만 기사 조회 수를 증가시킨다.
    ArticleView articleView = articleViewRepository.findByArticleIdAndUserId(articleId, requestUserId)
        .map(existingArticleView -> {
          existingArticleView.touch();
          return existingArticleView;
        })
        .orElseGet(() -> {
          isFirstView.set(true);
          return createFirstViewSafely(article, user);
        });

    if (isFirstView.get()){
      eventPublisher.publishEvent(ArticleViewEvent.from(articleView));
    }
    log.debug("기사 뷰 등록 성공 - articleId={}, requestUserId={}, articleViewId={}",
        articleId, requestUserId, articleView.getId());

    return articleViewMapper.toArticleViewDto(articleView);
  }

  // 첫 조회 저장을 안전하게 처리하고 조회 수를 반영한다.
  private ArticleView createFirstViewSafely(NewsArticle article, User user) {
    try {
      ArticleView savedArticleView = articleViewRepository.saveAndFlush(ArticleView.create(article, user));
      newsArticleRepository.incrementViewCountById(article.getId());
      entityManager.refresh(article);
      return savedArticleView;
    } catch (DataIntegrityViolationException e) {
      ArticleView existingArticleView = articleViewRepository.findByArticleIdAndUserId(
              article.getId(), user.getId())
          .orElseThrow(() -> e);
      existingArticleView.touch();
      entityManager.refresh(article);
      return existingArticleView;
    }
  }
  
  // 조회 가능한 기사만 반환한다.
  private NewsArticle findActiveArticle(UUID articleId) {
    NewsArticle article = newsArticleRepository.findById(articleId)
        .orElseThrow(() -> new ArticleNotFoundException(articleId));

    if (article.isDeleted()) {
      throw new DeletedArticleException(articleId);
    }

    return article;
  }

  // 조회 가능한 사용자만 반환한다.
  private User findActiveUser(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));

    if (user.isDeleted()) {
      throw new DeletedUserException(userId);
    }

    return user;
  }
}
