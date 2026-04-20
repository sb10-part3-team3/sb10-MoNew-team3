package com.team3.monew.entity;

import com.team3.monew.entity.base.BaseEntity;
import com.team3.monew.entity.enums.NotificationResourceType;
import com.team3.monew.entity.enums.NotificationType;
import jakarta.persistence.*;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

  // 알림 받는 사용자
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  // 알림 메시지
  @Column(nullable = false, length = 500)
  private String content;

  // 리소스 타입 (INTEREST, COMMENT)
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private NotificationResourceType resourceType;

  // 리소스 ID (FK 아님, 단순 식별자)
  @Column(name = "resource_id", nullable = false)
  private UUID resourceId;

  // 행위한 사용자 (좋아요 누른 사람 등)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "actor_user_id")
  private User actorUser;

  // 확인 여부
  @Column(name = "is_confirmed", nullable = false)
  private boolean isConfirmed = false;

  // 확인 시각
  @Column(name = "confirmed_at")
  private Instant confirmedAt;

  public static Notification create(User user, String content,
      NotificationResourceType resourceType, UUID resourceId, User actorUser) {
    return new Notification(user, content, resourceType, resourceId, actorUser);
  }

  private Notification(User user, String content, NotificationResourceType resourceType,
      UUID resourceId, User actorUser) {
    //필수값
    this.user = Objects.requireNonNull(user, "알림 수신 사용자는 필수입니다.");
    this.content = Objects.requireNonNull(content, "알림 내용은 필수입니다.");
    this.resourceType = Objects.requireNonNull(resourceType, "리소스 타입은 필수입니다.");
    this.resourceId = Objects.requireNonNull(resourceId, "리소스 ID는 필수입니다.");
    // actorUser는 댓글좋아요에서만 존재
    if (resourceType == NotificationResourceType.COMMENT) {
      this.actorUser = Objects.requireNonNull(actorUser, "좋아요 알람에서 좋아요를 누른 사용자는 필수입니다.");
    } else {
      this.actorUser = actorUser;
    }
    this.isConfirmed = false;
  }

}
