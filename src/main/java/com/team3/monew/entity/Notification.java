package com.team3.monew.entity;

import com.team3.monew.entity.base.BaseEntity;
import com.team3.monew.entity.enums.NotificationResourceType;
import com.team3.monew.entity.enums.NotificationType;
import jakarta.persistence.*;
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

    // 알림 타입
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType notificationType;

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

    public static Notification createCommentNotification(
            User user,
            String content,
            UUID commentId,
            User actorUser
    ) {
        Notification notification = new Notification();
        notification.user = user;
        notification.notificationType = NotificationType.COMMENT_LIKE;
        notification.content = content;
        notification.resourceType = NotificationResourceType.COMMENT;
        notification.resourceId = commentId;
        notification.actorUser = actorUser;

        return notification;
    }

    public static Notification createInterestNotification(
            User user,
            String content,
            UUID interestId,
            User actorUser
    ) {
        Notification notification = new Notification();
        notification.user = user;
        notification.notificationType = NotificationType.NEW_ARTICLE;
        notification.content = content;
        notification.resourceType = NotificationResourceType.INTEREST;
        notification.resourceId = interestId;
        notification.actorUser = actorUser;

        return notification;
    }
}
