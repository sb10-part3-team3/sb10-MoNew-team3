package com.team3.monew.service;

import com.team3.monew.dto.article.ArticleDto;
import com.team3.monew.dto.article.ArticleSearchRequest;
import com.team3.monew.dto.article.internal.ArticleCursor;
import com.team3.monew.dto.article.internal.ArticleSearchCondition;
import com.team3.monew.dto.pagination.CursorPageResponseDto;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.global.enums.ErrorCode;
import com.team3.monew.global.exception.BusinessException;
import com.team3.monew.mapper.ArticleMapper;
import com.team3.monew.repository.ArticleViewRepository;
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

  private static final String CURSOR_DELIMITER = ", ";

  public CursorPageResponseDto<ArticleDto> getArticleList(
      ArticleSearchRequest request, UUID requestUserId) {
    log.debug("뉴스 목록 조회 요청 - keyword={}, interestid={}", request.keyword(), request.interestId());
    ArticleCursor cursor = parseCursor(request);
    ArticleSearchCondition searchCondition = articleMapper.toCondition(request, cursor);

    List<NewsArticle> articles = newsArticleRepository.searchByCondition(searchCondition);
    Long totalElements = newsArticleRepository.countByCondition(searchCondition);

    if (articles.isEmpty()) {
      log.debug("뉴스 목록 조회 성공 - keyword={}, interestid={}, size=0",
          request.keyword(), request.interestId());
      return new CursorPageResponseDto<>(Collections.emptyList(), null, null, 0, totalElements,
          false);
    }

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
}
