package com.team3.monew.repository.impl;

import static com.team3.monew.entity.QArticleInterest.articleInterest;
import static com.team3.monew.entity.QNewsArticle.newsArticle;
import static com.team3.monew.entity.QNewsSource.newsSource;
import static org.springframework.util.StringUtils.hasText;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team3.monew.dto.article.internal.ArticleCursor;
import com.team3.monew.dto.article.internal.ArticleSearchCondition;
import com.team3.monew.dto.article.internal.enums.ArticleDirection;
import com.team3.monew.dto.article.internal.enums.ArticleOrderBy;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.enums.DeleteStatus;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.repository.NewsArticleRepositoryCustom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NewsArticleRepositoryImpl implements NewsArticleRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public Long countByCondition(ArticleSearchCondition cond) {

    JPAQuery<Long> query = queryFactory
        .select(newsArticle.countDistinct())
        .from(newsArticle)
        .join(newsArticle.source, newsSource);

    if (cond.interestId() != null) {
      query.join(newsArticle.articleInterests, articleInterest);
    }

    Long value = query
        .where(searchPredicate(cond))
        .fetchOne();

    return value != null ? value : 0L;
  }

  @Override
  public List<NewsArticle> searchByCondition(ArticleSearchCondition cond) {

    JPAQuery<NewsArticle> query = queryFactory
        .selectFrom(newsArticle)
        .distinct()
        .join(newsArticle.source, newsSource);

    if (cond.interestId() != null) {
      query.join(newsArticle.articleInterests, articleInterest);
    }

    return query
        .where(searchPredicate(cond))      // 일반 조건
        .where(cursorCondition(cond.cursor(), cond.articleOrderBy(), cond.direction())) // 커서 조건
        .orderBy(
            articleSort(cond.articleOrderBy(), cond.direction()).toArray(
                OrderSpecifier[]::new)
        )
        .limit(cond.limit() + 1L)
        .fetch();
  }

  private BooleanExpression[] searchPredicate(ArticleSearchCondition condition) {
    return new BooleanExpression[]{
        deleteStatusEqActive(),
        interestIdEq(condition.interestId()),
        titleOrSummaryContains(condition.keyword()),
        newsSourceTypeIn(condition.sourceIn()),
        publishedDateBetween(condition)
    };
  }

  private BooleanExpression deleteStatusEqActive() {
    return newsArticle.deleteStatus.eq(DeleteStatus.ACTIVE);
  }

  private BooleanExpression interestIdEq(UUID uuid) {
    return uuid != null ? articleInterest.interest.id.eq(uuid) : null;
  }

  private BooleanExpression titleOrSummaryContains(String keyword) {
    return hasText(keyword)
        ? newsArticle.title.contains(keyword).or(newsArticle.summary.contains(keyword))
        : null;
  }

  private BooleanExpression publishedDateBetween(ArticleSearchCondition condition) {
    BooleanExpression from = condition.publishDateFrom() != null
        ? newsArticle.publishedAt.goe(condition.publishDateFrom()) : null;
    BooleanExpression to = condition.publishDateTo() != null
        ? newsArticle.publishedAt.loe(condition.publishDateTo()) : null;

    if (from == null && to == null) {
      return null;
    } else if (from == null) {
      return to;
    } else if (to == null) {
      return from;
    }
    return from.and(to);
  }

  private BooleanExpression newsSourceTypeIn(List<NewsSourceType> types) {
    if (types == null || types.isEmpty()) {
      return null;
    }

    return newsSource.sourceType.in(types);
  }

  private BooleanExpression cursorCondition(ArticleCursor cursor, ArticleOrderBy articleOrderBy,
      ArticleDirection direction) {
    if (cursor == null) {
      return null;
    }

    boolean isDesc = direction == ArticleDirection.DESC;
    Instant after = cursor.after();

    return switch (articleOrderBy) {
      case PUBLISH_DATE -> {
        Instant lastTime = (Instant) cursor.cursor();

        yield isDesc
            ? newsArticle.publishedAt.lt(lastTime)
            .or(newsArticle.publishedAt.eq(lastTime).and(newsArticle.createdAt.lt(after)))
            : newsArticle.publishedAt.gt(lastTime)
                .or(newsArticle.publishedAt.eq(lastTime).and(newsArticle.createdAt.gt(after)));
      }
      case COMMENT_COUNT -> {
        int lastComment = (int) cursor.cursor();

        yield isDesc
            ? newsArticle.commentCount.lt(lastComment)
            .or(newsArticle.commentCount.eq(lastComment).and(newsArticle.createdAt.lt(after)))
            : newsArticle.commentCount.gt(lastComment)
                .or(newsArticle.commentCount.eq(lastComment).and(newsArticle.createdAt.gt(after)));
      }
      case VIEW_COUNT -> {
        int lastViewCount = (int) cursor.cursor();

        yield isDesc
            ? newsArticle.viewCount.lt(lastViewCount)
            .or(newsArticle.viewCount.eq(lastViewCount).and(newsArticle.createdAt.lt(after)))
            : newsArticle.viewCount.gt(lastViewCount)
                .or(newsArticle.viewCount.eq(lastViewCount).and(newsArticle.createdAt.gt(after)));
      }
    };
  }

  private List<OrderSpecifier<?>> articleSort(ArticleOrderBy articleOrderBy,
      ArticleDirection direction) {
    List<OrderSpecifier<?>> orders = new ArrayList<>();
    Order order = direction.equals(ArticleDirection.ASC) ? Order.ASC : Order.DESC;

    OrderSpecifier<?> orderSpecifier = switch (articleOrderBy) {
      case PUBLISH_DATE -> new OrderSpecifier<>(order, newsArticle.publishedAt);
      case COMMENT_COUNT -> new OrderSpecifier<>(order, newsArticle.commentCount);
      case VIEW_COUNT -> new OrderSpecifier<>(order, newsArticle.viewCount);
    };

    orders.add(orderSpecifier);
    orders.add(new OrderSpecifier<>(order, newsArticle.createdAt)); // 2차 정렬로 생성시간 역순

    return orders;
  }
}
