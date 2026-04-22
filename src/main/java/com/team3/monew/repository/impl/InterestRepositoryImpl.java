package com.team3.monew.repository.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team3.monew.dto.interest.internal.InterestSearchCondition;
import com.team3.monew.entity.Interest;
import com.team3.monew.entity.QInterest;
import com.team3.monew.entity.QInterestKeyword;
import com.team3.monew.repository.InterestRepositoryCustom;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.ast.Or;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InterestRepositoryImpl implements InterestRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<Interest> searchByCondition(InterestSearchCondition condition) {
    QInterest interest = QInterest.interest;
    QInterestKeyword keyword = QInterestKeyword.interestKeyword;

    return queryFactory
        .selectDistinct(interest)
        .from(interest)
        .leftJoin(interest.keywords, keyword)
        .where(
            buildSearchCondition(condition, interest, keyword),
            buildCursorCondition(condition, interest)
        )
        .orderBy(getOrderSpecifier(condition, interest))
        .limit(condition.limit() + 1L)
        .fetch();
  }

  @Override
  public int countByCondition(InterestSearchCondition condition) {
    QInterest interest = QInterest.interest;
    QInterestKeyword keyword = QInterestKeyword.interestKeyword;

    BooleanBuilder builder = buildSearchCondition(condition, interest, keyword);

    Long count = queryFactory
        .select(interest.countDistinct())
        .from(interest)
        .leftJoin(interest.keywords, keyword)
        .where(builder)
        .fetchOne();

    return count == null ? 0 : count.intValue();
  }

  private BooleanBuilder buildSearchCondition(
      InterestSearchCondition condition,
      QInterest interest,
      QInterestKeyword keyword
  ) {
    BooleanBuilder builder = new BooleanBuilder();

    String searchKeyword = condition.keyword();

    if (searchKeyword == null || searchKeyword.isBlank()) {
      return builder;
    }

    builder.and(
        interest.name.containsIgnoreCase(searchKeyword)
            .or(keyword.keyword.containsIgnoreCase(searchKeyword))
    );

    return builder;
  }

  private OrderSpecifier<?>[] getOrderSpecifier(
      InterestSearchCondition condition,
      QInterest interest
  ) {
    boolean isDesc = "DESC".equalsIgnoreCase(condition.direction());

    if ("subscriberCount".equals(condition.orderBy())) {
      return isDesc
          ? new OrderSpecifier[]{interest.subscriberCount.desc(), interest.createdAt.desc()}
          : new OrderSpecifier[]{interest.subscriberCount.asc(), interest.createdAt.asc()};
    }

    return isDesc
        ? new OrderSpecifier[]{interest.name.desc(), interest.createdAt.desc()}
        : new OrderSpecifier[]{interest.name.asc(), interest.createdAt.asc()};
  }

  private BooleanExpression buildCursorCondition(
      InterestSearchCondition condition,
      QInterest interest
  ) {
    if (condition.cursor() == null) {
      return null;
    }

    String cursorValue = condition.cursor().cursor();
    Instant after = condition.cursor().after();

    if (cursorValue == null || after == null) {
      return null;
    }

    String orderBy = condition.orderBy();
    boolean isDesc = "DESC".equalsIgnoreCase(condition.direction());

    if ("subscriberCount".equals(orderBy)) {
      int cursorCount = Integer.parseInt(cursorValue);

      if (isDesc) {
        return interest.subscriberCount.lt(cursorCount)
            .or(
                interest.subscriberCount.eq(cursorCount)
                    .and(interest.createdAt.lt(after))
            );
      }

      return interest.subscriberCount.gt(cursorCount)
          .or(
              interest.subscriberCount.eq(cursorCount)
                  .and(interest.createdAt.lt(after))
          );
    }

    // 기본: name 정렬
    if (isDesc) {
      return interest.name.lt(cursorValue)
          .or(
              interest.name.eq(cursorValue)
                  .and(interest.createdAt.lt(after
                  ))
          );
    }

    return interest.name.gt(cursorValue)
        .or(
            interest.name.eq(cursorValue)
                .and(interest.createdAt.gt(after))
        );
  }
}