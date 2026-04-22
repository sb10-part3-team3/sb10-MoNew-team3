package com.team3.monew.service;

import com.team3.monew.dto.comment.CommentDto;
import com.team3.monew.dto.comment.CommentRegisterRequest;
import com.team3.monew.dto.comment.CommentUpdateRequest;
import com.team3.monew.dto.comment.CursorPageResponseCommentDto;
import com.team3.monew.entity.Comment;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.User;
import com.team3.monew.entity.enums.NotificationResourceType;
import com.team3.monew.exception.article.ArticleNotFoundException;
import com.team3.monew.exception.article.DeletedArticleException;
import com.team3.monew.exception.comment.CommentException;
import com.team3.monew.exception.comment.CommentNotFoundException;
import com.team3.monew.exception.comment.DeletedCommentException;
import com.team3.monew.exception.comment.UnauthorizedCommentDeleteException;
import com.team3.monew.exception.comment.UnauthorizedCommentUpdateException;
import com.team3.monew.exception.user.DeletedUserException;
import com.team3.monew.exception.user.UserNotFoundException;
import com.team3.monew.global.enums.ErrorCode;
import com.team3.monew.global.exception.BusinessException;
import com.team3.monew.mapper.CommentMapper;
import com.team3.monew.repository.CommentLikeRepository;
import com.team3.monew.repository.CommentRepository;
import com.team3.monew.repository.NewsArticleRepository;
import com.team3.monew.repository.NotificationRepository;
import com.team3.monew.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  private static final String ORDER_BY_LIKE_COUNT = "likeCount";
  private static final String CURSOR_DELIMITER = "|";

  private final UserRepository userRepository;
  private final CommentRepository commentRepository;
  private final CommentLikeRepository commentLikeRepository;
  private final CommentMapper commentMapper;
  private final NewsArticleRepository newsArticleRepository;
  private final NotificationRepository notificationRepository;

  @Transactional
  public CommentDto registerComment(CommentRegisterRequest request) {
    log.debug("댓글 등록 요청 처리 시작: articleId={}, userId={}", request.articleId(), request.userId());

    NewsArticle article = findActiveArticle(request.articleId());
    User user = findActiveUser(request.userId());

    Comment comment = Comment.create(article, user, request.content());
    Comment savedComment = commentRepository.save(comment);
    newsArticleRepository.incrementCommentCountById(request.articleId());

    CommentDto commentDto = commentMapper.toDto(savedComment, false);
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

    Comment comment = findActiveComment(commentId);
    validateCommentAuthor(
        comment,
        requestUserId,
        new UnauthorizedCommentUpdateException(commentId)
    );

    comment.updateContent(request.content());
    CommentDto commentDto = commentMapper.toDto(comment, false);
    log.debug("댓글 수정 서비스 종료: commentId={}, requestUserId={}", commentId, requestUserId);
    return commentDto;
  }

  @Transactional
  public void deleteComment(UUID commentId, UUID requestUserId) {
    log.debug("댓글 논리 삭제 요청 처리 시작: commentId={}, requestUserId={}", commentId, requestUserId);

    Comment comment = findActiveComment(commentId);
    validateCommentAuthor(
        comment,
        requestUserId,
        new UnauthorizedCommentDeleteException(commentId)
    );

    comment.markDeleted();
    newsArticleRepository.decrementCommentCountById(comment.getArticle().getId());
    log.debug("댓글 논리 삭제 완료: commentId={}, requestUserId={}", commentId, requestUserId);
  }

  @Transactional
  public void hardDeleteComment(UUID commentId) {
    log.debug("댓글 물리 삭제 요청 처리 시작: commentId={}", commentId);

    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new CommentNotFoundException(commentId));

    if (!comment.isDeleted()) {
      newsArticleRepository.decrementCommentCountById(comment.getArticle().getId());
    }
    commentLikeRepository.deleteByCommentId(commentId);
    notificationRepository.deleteByResourceTypeAndResourceId(
        NotificationResourceType.COMMENT,
        commentId
    );
    commentRepository.delete(comment);
    log.debug("댓글 물리 삭제 완료: commentId={}", commentId);
  }

  public CursorPageResponseCommentDto findAll(UUID articleId, String orderBy, String cursor,
      Instant after, int limit, UUID requestUserId) {
    log.debug("댓글 목록 조회 요청 처리 시작: articleId={}, requestUserId={}", articleId, requestUserId);
    validateLimit(limit);

    List<Comment> comments = commentRepository.findActiveComments(
        articleId, orderBy, cursor, after, limit + 1);
    long totalElements = commentRepository.countActiveComments(articleId);
    boolean hasNext = comments.size() > limit;
    List<Comment> pageComments = hasNext ? comments.subList(0, limit) : comments;

    if (pageComments.isEmpty()) {
      log.debug("댓글 목록 조회 완료: articleId={}, size=0, hasNext=false", articleId);
      return new CursorPageResponseCommentDto(List.of(), null, null, 0, totalElements, false);
    }

    List<UUID> commentIds = pageComments.stream()
        .map(Comment::getId)
        .toList();
    Set<UUID> likedCommentIds = commentLikeRepository.findLikedCommentIds(requestUserId, commentIds);
    List<CommentDto> content = pageComments.stream()
        .map(comment -> commentMapper.toDto(comment, likedCommentIds.contains(comment.getId())))
        .toList();

    Comment lastComment = pageComments.get(pageComments.size() - 1);
    String nextCursor = hasNext ? resolveNextCursor(lastComment, orderBy) : null;
    Instant nextAfter = hasNext ? lastComment.getCreatedAt() : null;

    log.debug("댓글 목록 조회 완료: articleId={}, size={}, hasNext={}",
        articleId, content.size(), hasNext);
    return new CursorPageResponseCommentDto(
        content, nextCursor, nextAfter, content.size(), totalElements, hasNext);
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

  private Comment findActiveComment(UUID commentId) {
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new CommentNotFoundException(commentId));

    if (comment.isDeleted()) {
      throw new DeletedCommentException(commentId);
    }
    return comment;
  }

  private void validateCommentAuthor(
      Comment comment,
      UUID requestUserId,
      CommentException unauthorizedException
  ) {
    UUID authorId = comment.getUser().getId();
    if (comment.getUser().isDeleted()) {
      throw new DeletedUserException(authorId);
    }

    if (!authorId.equals(requestUserId)) {
      throw unauthorizedException;
    }
  }

  private String resolveNextCursor(Comment comment, String orderBy) {
    if (ORDER_BY_LIKE_COUNT.equals(orderBy)) {
      return String.join(
          CURSOR_DELIMITER,
          String.valueOf(comment.getLikeCount()),
          comment.getCreatedAt().toString()
      );
    }
    return comment.getCreatedAt().toString();
  }

  private void validateLimit(int limit) {
    if (limit < 1) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT_VALUE,
          Map.of("limit", String.valueOf(limit))
      );
    }
  }
}
