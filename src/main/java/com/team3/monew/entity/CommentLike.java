package com.team3.monew.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(
    name = "comment_likes",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_comment_likes_comment_id_user_id", columnNames = {"comment_id",
            "user_id"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentLike {

  @Id
  @GeneratedValue
  private UUID id;

  @OnDelete(action = OnDeleteAction.CASCADE)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "comment_id", nullable = false)
  private Comment comment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false)
  private Instant createdAt;

  public static CommentLike create(Comment comment, User user) {
    CommentLike commentLike = new CommentLike();
    commentLike.comment = comment;
    commentLike.user = user;
    commentLike.createdAt = Instant.now();

    return commentLike;
  }
}
