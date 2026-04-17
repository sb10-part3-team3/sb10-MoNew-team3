package com.team3.monew.service;

import com.team3.monew.dto.data.CommentDto;
import com.team3.monew.dto.request.CommentRegisterRequest;
import com.team3.monew.entity.Comment;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.User;
import com.team3.monew.exception.article.ArticleNotFoundException;
import com.team3.monew.exception.article.DeletedArticleException;
import com.team3.monew.exception.user.DeletedUserException;
import com.team3.monew.exception.user.UserNotFoundException;
import com.team3.monew.mapper.CommentMapper;
import com.team3.monew.repository.CommentRepository;
import com.team3.monew.repository.NewsArticleRepository;
import com.team3.monew.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

  private final UserRepository userRepository;
  private final CommentRepository commentRepository;
  private final CommentMapper commentMapper;
  private final NewsArticleRepository newsArticleRepository;

  @Transactional
  public CommentDto registerComment(CommentRegisterRequest request) {
    NewsArticle article = findActiveArticle(request.articleId());
    User user = findActiveUser(request.userId());

    Comment comment = Comment.create(article, user, request.content());
    Comment savedComment = commentRepository.save(comment);
    article.increaseCommentCount();

    return commentMapper.toDto(savedComment, false);
  }

  private NewsArticle findActiveArticle(UUID articleId) {
    NewsArticle article = newsArticleRepository.findById(articleId)
        .orElseThrow(() -> new ArticleNotFoundException(articleId));

    if (article.isDeleted()) {
      throw new DeletedArticleException(articleId);
    }
    return article;
  }

  private User findActiveUser(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));

    if (user.isDeleted()) {
      throw new DeletedUserException(userId);
    }
    return user;
  }
}
