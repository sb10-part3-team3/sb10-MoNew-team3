package com.team3.monew.entity;

import com.team3.monew.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "interests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Interest extends BaseEntity {

  @Column(nullable = false, unique = true, length = 100)
  private String name;

  @Column(nullable = false)
  private int subscriberCount;

  @OneToMany(mappedBy = "interest", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<InterestKeyword> keywords = new ArrayList<>();

  @OneToMany(mappedBy = "interest")
  private List<Subscription> subscriptions = new ArrayList<>();

  @OneToMany(mappedBy = "interest")
  private List<ArticleInterest> articleInterests = new ArrayList<>();

  public static Interest create(String name) {
    Interest interest = new Interest();
    interest.name = name;
    interest.subscriberCount = 0;

    return interest;
  }

  // keyword는 해당 메서드를 통해 서비스에서 따로 붙임 -> entity에 책임이 몰리는 것을 방지하기 위함
  public void addKeyword(String keyword) {
    InterestKeyword interestKeyword = InterestKeyword.create(this, keyword);
    this.keywords.add(interestKeyword);
  }

  public void updateKeywords(List<String> newKeywords) {
    this.keywords.clear();
    for (String keyword : newKeywords) {
      addKeyword(keyword);
    }
  }

  public List<String> getStringKeywords() {
    return this.keywords.stream()
        .map(InterestKeyword::getKeyword)
        .toList();
  }
}
