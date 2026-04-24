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
import com.team3.monew.event.CommentRegisteredEvent;
import com.team3.monew.event.CommentLikedEvent;
import com.team3.monew.exception.article.ArticleNotFoundException;
import com.team3.monew.exception.article.DeletedArticleException;
import com.team3.monew.exception.comment.CommentException;
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

  private static final String ORDER_BY_CREATED_AT = "createdAt";
  private static final String ORDER_BY_LIKE_COUNT = "likeCount";
  private static final String CURSOR_DELIMITER = "|";

  private record CommentCursor(Integer likeCount, Instant createdAt) {
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
    log.debug("댓글 등록 요청 처리 시작: articleId={}, userId={}", request.articleId(), request.userId());

    NewsArticle article = findActiveArticle(request.articleId());
    User user = findActiveUser(request.userId());

    Comment comment = Comment.create(article, user, request.content());
    Comment savedComment = commentRepository.save(comment);
    newsArticleRepository.incrementCommentCountById(request.articleId());

    CommentDto commentDto = commentMapper.toDto(savedComment, false);

    // 댓글 등록 이벤트 발행
    eventPublisher.publishEvent(CommentRegisteredEvent.from(savedComment));
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
  public void deleteComment(UUID commentId) {
    log.debug("댓글 논리 삭제 요청 처리 시작: commentId={}", commentId);

    Comment comment = findActiveComment(commentId);
    comment.markDeleted();
    newsArticleRepository.decrementCommentCountById(comment.getArticle().getId());
    log.debug("댓글 논리 삭제 완료: commentId={}", commentId);
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
    if (limit < 1) {
      throwInvalidInput("limit", limit);
    }

    List<Comment> comments = findActiveComments(articleId, orderBy, cursor, after, limit + 1);
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

  @Transactional
  public CommentLikeDto likeComment(UUID commentId, UUID requestUserId) {
    log.debug("댓글 좋아요 등록 요청 처리 시작: commentId={}, requestUserId={}", commentId, requestUserId);

    Comment comment = findActiveComment(commentId);
    User user = findActiveUser(requestUserId);

    if (commentLikeRepository.existsByCommentIdAndUserId(commentId, requestUserId)) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT_VALUE,
          Map.of("commentLike", "alreadyExists")
      );
    }

    CommentLike savedCommentLike = commentLikeRepository.save(CommentLike.create(comment, user));
    comment.increaseLikeCount();

    if (!comment.getUser().getId().equals(requestUserId)) {
      eventPublisher.publishEvent(new CommentLikedEvent(requestUserId, commentId));
    }

    log.debug("댓글 좋아요 등록 완료: commentId={}, requestUserId={}", commentId, requestUserId);
    return toCommentLikeDto(savedCommentLike);
  }

  @Transactional
  public void unlikeComment(UUID commentId, UUID requestUserId) {
    log.debug("댓글 좋아요 취소 요청 처리 시작: commentId={}, requestUserId={}", commentId, requestUserId);

    Comment comment = findActiveComment(commentId);
    CommentLike commentLike = commentLikeRepository.findByCommentIdAndUserId(commentId, requestUserId)
        .orElseThrow(() -> new BusinessException(
            ErrorCode.INVALID_INPUT_VALUE,
            Map.of("commentLike", "notFound")
        ));

    comment.decreaseLikeCount();
    commentLikeRepository.delete(commentLike);

    log.debug("댓글 좋아요 취소 완료: commentId={}, requestUserId={}", commentId, requestUserId);
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

  // 활성 기사인지 확인하고 반환한다.
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

  // 활성 사용자인지 확인하고 반환한다.
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

  // 활성 댓글인지 확인하고 반환한다.
  private Comment findActiveComment(UUID commentId) {
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new CommentNotFoundException(commentId));

    if (comment.isDeleted()) {
      throw new DeletedCommentException(commentId);
    }
    return comment;
  }

  // 댓글 작성자와 요청자가 같은지 확인한다.
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

  // 다음 페이지 조회에 사용할 커서 값을 만든다.
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

  // 정렬 기준에 맞는 댓글 조회 쿼리를 선택한다.
  private List<Comment> findActiveComments(
      UUID articleId,
      String orderBy,
      String cursor,
      Instant after,
      int limit
  ) {
    Pageable pageable = PageRequest.of(0, limit);
    CommentCursor commentCursor = parseCursor(orderBy, cursor, after);

    if (ORDER_BY_CREATED_AT.equals(orderBy)) {
      return commentRepository.findActiveCommentsByCreatedAtDesc(
          articleId,
          commentCursor.createdAt(),
          pageable
      );
    }

    if (ORDER_BY_LIKE_COUNT.equals(orderBy)) {
      return commentRepository.findActiveCommentsByLikeCountDesc(
          articleId,
          commentCursor.likeCount(),
          commentCursor.createdAt(),
          pageable
      );
    }

    throwInvalidInput("orderBy", orderBy);
    return List.of();
  }

  // 요청 커서를 정렬 기준값으로 해석한다.
  private CommentCursor parseCursor(String orderBy, String cursor, Instant after) {
    String[] cursorValues = splitCursor(cursor);

    if (ORDER_BY_CREATED_AT.equals(orderBy)) {
      return new CommentCursor(null, parseInstant(cursorValue(cursorValues, 0), "cursor"));
    }

    if (ORDER_BY_LIKE_COUNT.equals(orderBy)) {
      Instant cursorCreatedAt = parseInstant(cursorValue(cursorValues, 1), "cursorCreatedAt");
      return new CommentCursor(
          parseInteger(cursorValue(cursorValues, 0), "cursor"),
          cursorCreatedAt != null ? cursorCreatedAt : after
      );
    }

    throwInvalidInput("orderBy", orderBy);
    return new CommentCursor(null, null);
  }

  // 커서를 구분자로 나누고 빈 커서는 빈 배열로 반환한다.
  private String[] splitCursor(String cursor) {
    if (cursor == null || cursor.isBlank()) {
      return new String[0];
    }
    return cursor.split(Pattern.quote(CURSOR_DELIMITER));
  }

  // 커서 배열에서 지정 위치 값을 꺼낸다.
  private String cursorValue(String[] cursorValues, int index) {
    if (cursorValues.length <= index) {
      return null;
    }
    return cursorValues[index];
  }

  // 문자열을 Instant로 변환하고 잘못된 값은 입력 오류로 처리한다.
  private Instant parseInstant(String value, String field) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Instant.parse(value);
    } catch (DateTimeParseException e) {
      throwInvalidInput(field, value);
      return null;
    }
  }

  // 문자열을 Integer로 변환하고 잘못된 값은 입력 오류로 처리한다.
  private Integer parseInteger(String value, String field) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throwInvalidInput(field, value);
      return null;
    }
  }

  // 입력 오류 예외를 생성해 던진다.
  private void throwInvalidInput(String field, Object value) {
    throw new BusinessException(
        ErrorCode.INVALID_INPUT_VALUE,
        Map.of(field, String.valueOf(value))
    );
  }
}
