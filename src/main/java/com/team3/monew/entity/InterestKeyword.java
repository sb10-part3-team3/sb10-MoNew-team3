package com.team3.monew.entity;

import com.team3.monew.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "interest_keywords",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_interest_keywords_interest_id_keyword", columnNames = {
            "interest_id", "keyword"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterestKeyword extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "interest_id", nullable = false)
  private Interest interest;

  @Column(nullable = false, length = 100)
  private String keyword;

  public static InterestKeyword create(Interest interest, String keyword) {
    InterestKeyword interestKeyword = new InterestKeyword();
    interestKeyword.interest = interest;
    interestKeyword.keyword = keyword;

    return interestKeyword;
  }

  public void assignInterest(Interest interest) {
    this.interest = interest;
  }
}
