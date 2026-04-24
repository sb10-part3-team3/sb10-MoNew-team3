package com.team3.monew.service;

import com.team3.monew.dto.comment.CommentDto;
import com.team3.monew.dto.comment.CommentLikeDto;
import com.team3.monew.dto.comment.CommentRegisterRequest;
import com.team3.monew.dto.comment.CommentUpdateRequest;
import com.team3.monew.dto.comment.CursorPageResponseCommentDto;
import com.team3.monew.entity.Comment;
import com.team3.monew.entity.CommentLike;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.User;
import com.team3.monew.entity.enums.NotificationResourceType;
import com.team3.monew.event.CommentLikedEvent;
import com.team3.monew.exception.article.ArticleNotFoundException;
import com.team3.monew.exception.article.DeletedArticleException;
import com.team3.monew.exception.comment.CommentException;
import com.team3.monew.exception.comment.CommentLikeAlreadyExistsException;
import com.team3.monew.exception.comment.CommentLikeNotFoundException;
import com.team3.monew.exception.comment.CommentNotFoundException;
import com.team3.monew.exception.comment.DeletedCommentException;
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
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

  private static final String CURSOR_DELIMITER = "|";

  private enum CommentOrderBy {
    CREATED_AT,
    LIKE_COUNT
  }

  private enum CommentDirection {
    DESC
  }

  private record CommentCursor(Integer likeCount, Instant createdAt) {
  }

  private record CommentSearchCondition(
      UUID articleId,
      CommentOrderBy orderBy,
      CommentDirection direction,
      CommentCursor cursor,
      int limit,
      UUID requestUserId
  ) {
  }

  private final UserRepository userRepository;
  private final CommentRepository commentRepository;
  private final CommentLikeRepository commentLikeRepository;
  private final CommentMapper commentMapper;
  private final NewsArticleRepository newsArticleRepository;
  private final NotificationRepository notificationRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public CommentDto registerComment(CommentRegisterRequest request) {
    log.debug("Register comment request: articleId={}, userId={}",
        request.articleId(), request.userId());

    NewsArticle article = findActiveArticle(request.articleId());
    User user = findActiveUser(request.userId());

    Comment comment = Comment.create(article, user, request.content());
    Comment savedComment = commentRepository.save(comment);
    newsArticleRepository.incrementCommentCountById(request.articleId());

    return commentMapper.toDto(savedComment, false);
  }

  @Transactional
  public CommentDto updateComment(UUID commentId, UUID requestUserId, CommentUpdateRequest request) {
    log.debug("Update comment request: commentId={}, requestUserId={}", commentId, requestUserId);

    Comment comment = findActiveComment(commentId);
    validateCommentAuthor(comment, requestUserId, new UnauthorizedCommentUpdateException(commentId));

    comment.updateContent(request.content());
    return commentMapper.toDto(comment, false);
  }

  @Transactional
  public void deleteComment(UUID commentId) {
    log.debug("Delete comment request: commentId={}", commentId);

    Comment comment = findActiveComment(commentId);
    comment.markDeleted();
    newsArticleRepository.decrementCommentCountById(comment.getArticle().getId());
  }

  @Transactional
  public void hardDeleteComment(UUID commentId) {
    log.debug("Hard delete comment request: commentId={}", commentId);

    Comment comment = findComment(commentId);

    if (!comment.isDeleted()) {
      newsArticleRepository.decrementCommentCountById(comment.getArticle().getId());
    }

    commentLikeRepository.deleteByCommentId(commentId);
    notificationRepository.deleteByResourceTypeAndResourceId(
        NotificationResourceType.COMMENT,
        commentId
    );
    commentRepository.delete(comment);
  }

  public CursorPageResponseCommentDto findComments(
      UUID articleId,
      String orderBy,
      String direction,
      String cursor,
      Instant after,
      int limit,
      UUID requestUserId
  ) {
    CommentSearchCondition condition = createSearchCondition(
        articleId,
        orderBy,
        direction,
        cursor,
        after,
        limit,
        requestUserId
    );

    log.debug(
        "Find comments request: articleId={}, orderBy={}, direction={}, cursor={}, after={}, limit={}, requestUserId={}",
        articleId,
        orderBy,
        direction,
        cursor,
        after,
        limit,
        requestUserId
    );

    findReadableArticle(articleId);

    List<Comment> comments = findCommentPage(condition);
    long totalElements = commentRepository.countActiveComments(articleId);

    return buildCommentPageResponse(condition, comments, totalElements);
  }

  @Transactional
  public CommentLikeDto likeComment(UUID commentId, UUID requestUserId) {
    log.debug("Like comment request: commentId={}, requestUserId={}", commentId, requestUserId);

    Comment comment = findActiveComment(commentId);
    User user = findActiveUser(requestUserId);

    if (commentLikeRepository.existsByCommentIdAndUserId(commentId, requestUserId)) {
      throw new CommentLikeAlreadyExistsException();
    }

    CommentLike savedCommentLike = commentLikeRepository.save(CommentLike.create(comment, user));
    comment.increaseLikeCount();

    if (!comment.getUser().getId().equals(requestUserId)) {
      eventPublisher.publishEvent(new CommentLikedEvent(requestUserId, commentId));
    }

    return toCommentLikeDto(savedCommentLike);
  }

  @Transactional
  public void unlikeComment(UUID commentId, UUID requestUserId) {
    log.debug("Unlike comment request: commentId={}, requestUserId={}", commentId, requestUserId);

    Comment comment = findActiveComment(commentId);
    CommentLike commentLike = commentLikeRepository.findByCommentIdAndUserId(commentId, requestUserId)
        .orElseThrow(CommentLikeNotFoundException::new);

    comment.decreaseLikeCount();
    commentLikeRepository.delete(commentLike);
  }

  private CommentSearchCondition createSearchCondition(
      UUID articleId,
      String orderBy,
      String direction,
      String cursor,
      Instant after,
      int limit,
      UUID requestUserId
  ) {
    validateLimit(limit);
    CommentOrderBy parsedOrderBy = parseOrderBy(orderBy);

    return new CommentSearchCondition(
        articleId,
        parsedOrderBy,
        parseDirection(direction),
        parseCursor(parsedOrderBy, cursor, after),
        limit,
        requestUserId
    );
  }

  private CommentOrderBy parseOrderBy(String orderBy) {
    if ("createdAt".equals(orderBy)) {
      return CommentOrderBy.CREATED_AT;
    }

    if ("likeCount".equals(orderBy)) {
      return CommentOrderBy.LIKE_COUNT;
    }

    throw invalidInput("orderBy", orderBy);
  }

  private CommentDirection parseDirection(String direction) {
    if (direction == null || direction.isBlank() || "DESC".equalsIgnoreCase(direction)) {
      return CommentDirection.DESC;
    }

    throw invalidInput("direction", direction);
  }

  private CommentCursor parseCursor(CommentOrderBy orderBy, String cursor, Instant after) {
    String[] cursorValues = splitCursor(cursor);

    if (orderBy == CommentOrderBy.CREATED_AT) {
      return new CommentCursor(null, parseInstant(cursorValue(cursorValues, 0), "cursor", after));
    }

    return new CommentCursor(
        parseInteger(cursorValue(cursorValues, 0), "cursor"),
        parseInstant(cursorValue(cursorValues, 1), "cursorCreatedAt", after)
    );
  }

  private List<Comment> findCommentPage(CommentSearchCondition condition) {
    Pageable pageable = PageRequest.of(0, condition.limit() + 1);

    if (condition.orderBy() == CommentOrderBy.CREATED_AT) {
      return commentRepository.findActiveCommentsByCreatedAtDesc(
          condition.articleId(),
          condition.cursor().createdAt(),
          pageable
      );
    }

    return commentRepository.findActiveCommentsByLikeCountDesc(
        condition.articleId(),
        condition.cursor().likeCount(),
        condition.cursor().createdAt(),
        pageable
    );
  }

  private CursorPageResponseCommentDto buildCommentPageResponse(
      CommentSearchCondition condition,
      List<Comment> comments,
      long totalElements
  ) {
    boolean hasNext = comments.size() > condition.limit();
    List<Comment> pageComments = hasNext ? comments.subList(0, condition.limit()) : comments;

    if (pageComments.isEmpty()) {
      return new CursorPageResponseCommentDto(List.of(), null, null, 0, totalElements, false);
    }

    List<CommentDto> content = toCommentDtos(pageComments, condition.requestUserId());
    Comment lastComment = pageComments.get(pageComments.size() - 1);

    return new CursorPageResponseCommentDto(
        content,
        hasNext ? resolveNextCursor(lastComment, condition.orderBy()) : null,
        hasNext ? lastComment.getCreatedAt() : null,
        content.size(),
        totalElements,
        hasNext
    );
  }

  private List<CommentDto> toCommentDtos(List<Comment> comments, UUID requestUserId) {
    List<UUID> commentIds = comments.stream()
        .map(Comment::getId)
        .toList();

    Set<UUID> likedCommentIds = commentLikeRepository.findLikedCommentIds(requestUserId, commentIds);

    return comments.stream()
        .map(comment -> commentMapper.toDto(comment, likedCommentIds.contains(comment.getId())))
        .toList();
  }

  private String resolveNextCursor(Comment comment, CommentOrderBy orderBy) {
    if (orderBy == CommentOrderBy.LIKE_COUNT) {
      return String.join(
          CURSOR_DELIMITER,
          String.valueOf(comment.getLikeCount()),
          comment.getCreatedAt().toString()
      );
    }

    return comment.getCreatedAt().toString();
  }

  private CommentLikeDto toCommentLikeDto(CommentLike commentLike) {
    Comment comment = commentLike.getComment();

    return new CommentLikeDto(
        commentLike.getId(),
        commentLike.getUser().getId(),
        commentLike.getCreatedAt(),
        comment.getId(),
        comment.getArticle().getId(),
        comment.getUser().getId(),
        comment.getUser().getNickname(),
        comment.getContent(),
        (long) comment.getLikeCount(),
        comment.getCreatedAt()
    );
  }

  private NewsArticle findActiveArticle(UUID articleId) {
    return findArticle(articleId, () -> new ArticleNotFoundException(articleId));
  }

  private NewsArticle findReadableArticle(UUID articleId) {
    return findArticle(articleId, () -> invalidInput("articleId", articleId));
  }

  private NewsArticle findArticle(
      UUID articleId,
      Supplier<? extends RuntimeException> notFoundException
  ) {
    NewsArticle article = newsArticleRepository.findById(articleId)
        .orElseThrow(notFoundException);

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

  private Comment findActiveComment(UUID commentId) {
    Comment comment = findComment(commentId);

    if (comment.isDeleted()) {
      throw new DeletedCommentException(commentId);
    }

    return comment;
  }

  private Comment findComment(UUID commentId) {
    return commentRepository.findById(commentId)
        .orElseThrow(() -> new CommentNotFoundException(commentId));
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

  private void validateLimit(int limit) {
    if (limit < 1) {
      throw invalidInput("limit", limit);
    }
  }

  private String[] splitCursor(String cursor) {
    if (cursor == null || cursor.isBlank()) {
      return new String[0];
    }

    return cursor.split(Pattern.quote(CURSOR_DELIMITER));
  }

  private String cursorValue(String[] cursorValues, int index) {
    if (cursorValues.length <= index) {
      return null;
    }

    return cursorValues[index];
  }

  private Instant parseInstant(String value, String field, Instant defaultValue) {
    if (value == null || value.isBlank()) {
      return defaultValue;
    }

    try {
      return Instant.parse(value);
    } catch (DateTimeParseException e) {
      throw invalidInput(field, value);
    }
  }

  private Integer parseInteger(String value, String field) {
    if (value == null || value.isBlank()) {
      return null;
    }

    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw invalidInput(field, value);
    }
  }

  private BusinessException invalidInput(String field, Object value) {
    return new BusinessException(
        ErrorCode.INVALID_INPUT_VALUE,
        Map.of(field, String.valueOf(value))
    );
  }
}
