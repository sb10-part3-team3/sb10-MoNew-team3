package com.team3.monew.service;

import com.team3.monew.dto.comment.CommentDto;
import com.team3.monew.dto.comment.CommentRegisterRequest;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

  private final UserRepository userRepository;
  private final CommentRepository commentRepository;
  private final CommentMapper commentMapper;
  private final NewsArticleRepository newsArticleRepository;

  @Transactional
  public CommentDto registerComment(CommentRegisterRequest request, UUID userId) {
    log.debug("댓글 등록 요청 처리 시작: articleId={}, userId={}", request.articleId(), userId);

    NewsArticle article = findActiveArticle(request.articleId());
    User user = findActiveUser(userId);

    Comment comment = Comment.create(article, user, request.content());
    Comment savedComment = commentRepository.save(comment);
    newsArticleRepository.incrementCommentCountById(request.articleId());

    log.info(
        "댓글 등록 주요 로직 완료: articleId={}, userId={}, commentId={}",
        request.articleId(),
        userId,
        savedComment.getId()
    );

    CommentDto commentDto = commentMapper.toDto(savedComment, false);
    log.info(
        "댓글 등록 서비스 종료: articleId={}, userId={}, commentId={}",
        request.articleId(),
        userId,
        savedComment.getId()
    );
    return commentDto;
  }

  private NewsArticle findActiveArticle(UUID articleId) {
    log.debug("댓글 등록 기사 조회 시작: articleId={}", articleId);

    NewsArticle article = newsArticleRepository.findById(articleId)
        .orElseThrow(() -> new ArticleNotFoundException(articleId));

    if (article.isDeleted()) {
      throw new DeletedArticleException(articleId);
    }

    log.debug("댓글 등록 기사 조회 완료: articleId={}", articleId);
    return article;
  }

  private User findActiveUser(UUID userId) {
    log.debug("댓글 등록 사용자 조회 시작: userId={}", userId);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));

    if (user.isDeleted()) {
      throw new DeletedUserException(userId);
    }

    log.debug("댓글 등록 사용자 조회 완료: userId={}", userId);
    return user;
  }
}
