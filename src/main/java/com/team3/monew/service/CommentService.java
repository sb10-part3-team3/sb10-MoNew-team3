package com.team3.monew.service;

import com.team3.monew.dto.comment.CommentDto;
import com.team3.monew.dto.comment.CommentRegisterRequest;
import com.team3.monew.dto.comment.CommentUpdateRequest;
import com.team3.monew.entity.Comment;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.User;
import com.team3.monew.exception.article.ArticleNotFoundException;
import com.team3.monew.exception.article.DeletedArticleException;
import com.team3.monew.exception.comment.CommentNotFoundException;
import com.team3.monew.exception.comment.DeletedCommentException;
import com.team3.monew.exception.comment.UnauthorizedCommentException;
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
  public CommentDto registerComment(CommentRegisterRequest request) {
    log.debug("댓글 등록 요청 처리 시작: articleId={}, userId={}", request.articleId(), request.userId());

    NewsArticle article = findActiveArticle(request.articleId());
    User user = findActiveUser(request.userId());

    Comment comment = Comment.create(article, user, request.content());
    Comment savedComment = commentRepository.save(comment);
    newsArticleRepository.incrementCommentCountById(request.articleId());

    CommentDto commentDto = commentMapper.toDto(savedComment, false);
    log.info(
        "댓글 등록 완료: commentId={}, articleId={}",
        savedComment.getId(),
        request.articleId()
    );
    log.debug(
        "댓글 등록 서비스 종료: articleId={}, userId={}, commentId={}",
        request.articleId(),
        request.userId(),
        savedComment.getId()
    );
    return commentDto;
  }

  @Transactional
  public CommentDto updateComment(UUID commentId, UUID requestUserId, CommentUpdateRequest request) {
    log.debug("댓글 수정 요청 처리 시작: commentId={}, requestUserId={}", commentId, requestUserId);

    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new CommentNotFoundException(commentId));

    if (comment.isDeleted()) {
      throw new DeletedCommentException(commentId);
    }
    UUID authorId = comment.getUser().getId();
    if (comment.getUser().isDeleted()) {
      throw new DeletedUserException(authorId);
    }

    if (!authorId.equals(requestUserId)) {
      throw new UnauthorizedCommentException(commentId);
    }

    comment.updateContent(request.content());
    CommentDto commentDto = commentMapper.toDto(comment, false);
    log.info("댓글 수정 완료: commentId={}, requestUserId={}", commentId, requestUserId);
    log.debug("댓글 수정 서비스 종료: commentId={}, requestUserId={}", commentId, requestUserId);
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
