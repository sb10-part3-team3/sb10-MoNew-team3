package com.team3.monew.entity;

import com.team3.monew.entity.base.SoftDeleteEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends SoftDeleteEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private NewsArticle article;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 10000)
    private String content;

    @Column(nullable = false)
    private int likeCount;

    @OneToMany(mappedBy = "comment")
    private List<CommentLike> commentLikes = new ArrayList<>();

    public static Comment create(NewsArticle article, User user, String content) {
        Comment comment = new Comment();
        comment.article = article;
        comment.user = user;
        comment.content = content;
        comment.likeCount = 0;

        return comment;
    }

    public void updateContent(String content) {
        this.content = content;
    }
}
