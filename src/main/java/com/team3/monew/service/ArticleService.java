package com.team3.monew.service;

import com.team3.monew.dto.article.ArticleDto;
import com.team3.monew.dto.article.ArticleSearchRequest;
import com.team3.monew.dto.article.ArticleViewDto;
import com.team3.monew.dto.article.internal.ArticleCursor;
import com.team3.monew.dto.article.internal.ArticleSearchCondition;
import com.team3.monew.dto.pagination.CursorPageResponseDto;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.exception.article.ArticleNotFoundException;
import com.team3.monew.exception.article.DeletedArticleException;
import com.team3.monew.global.enums.ErrorCode;
import com.team3.monew.global.exception.BusinessException;
import com.team3.monew.mapper.ArticleMapper;
import com.team3.monew.repository.ArticleInterestRepository;
import com.team3.monew.repository.ArticleViewRepository;
import com.team3.monew.repository.CommentRepository;
import com.team3.monew.repository.NewsArticleRepository;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleService {

  private final ArticleMapper articleMapper;
  private final NewsArticleRepository newsArticleRepository;
  private final ArticleViewRepository articleViewRepository;
  private final ArticleViewService articleViewService;

  private static final String CURSOR_DELIMITER = ", ";
  private final ArticleInterestRepository articleInterestRepository;
  private final CommentRepository commentRepository;

  public CursorPageResponseDto<ArticleDto> getArticleList(
      ArticleSearchRequest request, UUID requestUserId) {
    log.debug("뉴스 목록 조회 요청 - keyword={}, interestid={}", request.keyword(), request.interestId());
    ArticleCursor cursor = parseCursor(request);
    ArticleSearchCondition searchCondition = articleMapper.toCondition(request, cursor);

    List<NewsArticle> articles = newsArticleRepository.searchByCondition(searchCondition);

    if (articles.isEmpty()) {
      log.debug("뉴스 목록 조회 성공 - keyword={}, interestid={}, size=0",
          request.keyword(), request.interestId());
      return new CursorPageResponseDto<>(Collections.emptyList(), null, null,
          0, (long) articles.size(), false);
    }

    Long totalElements = newsArticleRepository.countByCondition(searchCondition);
    boolean hasNext = articles.size() > request.limit();
    Object nextCursor = null;
    Instant nextAfter = null;
    if (hasNext) {
      articles = articles.subList(0, request.limit());
      NewsArticle lastArticle = articles.get(articles.size() - 1);
      nextCursor = switch (request.orderBy()) {
        case PUBLISH_DATE -> lastArticle.getPublishedAt();
        case COMMENT_COUNT -> lastArticle.getCommentCount();
        case VIEW_COUNT -> lastArticle.getViewCount();
      };
      // 커서(커서 반환값 + ", " + 커서필드 객체의 생성시간)
      nextCursor = nextCursor.toString() + CURSOR_DELIMITER + lastArticle.getCreatedAt();
      nextAfter = lastArticle.getCreatedAt();
    }
    log.debug("다음 커서 생성 완료: nextCursor={}, nextAfter={}", nextCursor, nextAfter);

    // article
    List<UUID> articleIds = articles.stream().map(NewsArticle::getId).toList();
    // requestUser가 읽은 기사 목록
    Set<UUID> viewedArticleIds = articleViewRepository.findAllByArticleIdInAndUserId(
        articleIds, requestUserId);

    List<ArticleDto> articleDtoList = articles.stream()
        .map(na -> articleMapper.toDto(na, viewedArticleIds.contains(na.getId())))
        .toList();

    log.debug("뉴스 목록 조회 성공 - keyword={}, interestid={}, size={}",
        request.keyword(), request.interestId(), articleDtoList.size());
    return new CursorPageResponseDto<>(articleDtoList,
        nextCursor != null ? nextCursor.toString() : null,
        nextAfter, articleDtoList.size(), totalElements, hasNext);
  }

  // 조회수 등록 때문에 readOnly=true 불가능으로 어노테이션 추가함
  @Transactional
  public ArticleDto getArticle(UUID userId, UUID articleId) {
    log.debug("뉴스 단건 조회 요청 - articleId={}", articleId);
    NewsArticle article = findActiveArticleOrElseThrow(articleId);
    // 단건 조회에도 조회수 등록을 위함
    articleViewService.registerArticleView(article.getId(), userId);

    // registerArticleView()에서 벌크 업데이트 수행으로 기존 엔티티의 viewCount가 갱신되지 않아 재조회
    NewsArticle updatedArticle = findActiveArticleOrElseThrow(articleId);

    log.debug("뉴스 단건 조회 성공 - articleId={}", updatedArticle.getId());
    return articleMapper.toDto(updatedArticle, true);
  }

  @Transactional
  public void deleteArticle(UUID articleId) {
    log.debug("뉴스기사 논리삭제 요청 - articleId={}", articleId);
    NewsArticle article = getArticleOrThrow(articleId);
    if (article.isDeleted()) {
      throw new ArticleNotFoundException(articleId);
    }

    article.markDeleted();
    newsArticleRepository.save(article);
    log.info("뉴스기사 논리삭제 성공 - articleId={}", articleId);
  }

  @Transactional
  public void hardDeleteArticle(UUID articleId) {
    log.debug("뉴스기사 물리삭제 요청 - articleId={}", articleId);
    NewsArticle newsArticle = getArticleOrThrow(articleId);

    // 1. ArticleInterest 삭제
    articleInterestRepository.deleteAllByArticleId(articleId);
    // 2. ArticleViews 삭제
    articleViewRepository.deleteAllByArticleId(articleId);
    // 3. Comments 삭제
    commentRepository.deleteAllByArticleId(articleId);

    newsArticleRepository.delete(newsArticle);
    log.info("뉴스기사 물리삭제 성공 - articleId={}", articleId);
  }

  private ArticleCursor parseCursor(ArticleSearchRequest request) {
    String cursor = request.cursor();
    if (cursor == null || cursor.isEmpty()) {
      return null;
    }

    String[] cursorSplit = cursor.split(CURSOR_DELIMITER);
    if (cursorSplit.length != 2) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
          Map.of("cursor", "Invalid cursor format"));
    }

    try {
      Object value = switch (request.orderBy()) {
        case PUBLISH_DATE -> Instant.parse(cursorSplit[0]);
        case COMMENT_COUNT, VIEW_COUNT -> Integer.valueOf(cursorSplit[0]);
      };
      Instant after = Instant.parse(cursorSplit[1]);

      return new ArticleCursor(value, after);
    } catch (DateTimeParseException | NumberFormatException e) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, Map.of("cursor", e.getMessage()));
    }
  }

  private NewsArticle findActiveArticleOrElseThrow(UUID articleId) {
    NewsArticle article = newsArticleRepository.findById(articleId)
        .orElseThrow(() -> new ArticleNotFoundException(articleId));

    // 논리삭제 여부 판단
    if (article.isDeleted()) {
      throw new DeletedArticleException(articleId);
    }

    return article;
  }

  private NewsArticle getArticleOrThrow(UUID articleId) {
    return newsArticleRepository.findById(articleId)
        .orElseThrow(() -> new ArticleNotFoundException(articleId));
  }
}
