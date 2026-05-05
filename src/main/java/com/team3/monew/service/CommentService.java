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
import com.team3.monew.event.CommentDeletedEvent;
import com.team3.monew.event.CommentLikedActivityEvent;
import com.team3.monew.event.CommentRegisteredEvent;
import com.team3.monew.event.CommentLikedEvent;
import com.team3.monew.event.CommentUnlikedEvent;
import com.team3.monew.event.CommentUpdatedEvent;
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

  private record CommentCursor(Integer likeCount, Instant createdAt, UUID id) {
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
    log.debug("댓글 등록 요청 - articleId={}, userId={}",
        request.articleId(), request.userId());

    NewsArticle article = findActiveArticle(request.articleId());
    User user = findActiveUser(request.userId());

    Comment comment = Comment.create(article, user, request.content());
    Comment savedComment = commentRepository.save(comment);
    newsArticleRepository.incrementCommentCountById(request.articleId());

    log.info("댓글 등록 성공 - commentId={}, articleId={}, userId={}",
        savedComment.getId(), request.articleId(), request.userId());
    // 댓글 등록 이벤트 발행
    eventPublisher.publishEvent(CommentRegisteredEvent.from(savedComment));
    return commentMapper.toDto(savedComment, false);
  }

  @Transactional
  public CommentDto updateComment(UUID commentId, UUID requestUserId, CommentUpdateRequest request) {
    log.debug("댓글 수정 요청 - commentId={}, requestUserId={}", commentId, requestUserId);

    Comment comment = findActiveComment(commentId);
    validateCommentAuthor(comment, requestUserId, new UnauthorizedCommentUpdateException(commentId));

    comment.updateContent(request.content());
    log.info("댓글 수정 성공 - commentId={}, requestUserId={}", commentId, requestUserId);
    eventPublisher.publishEvent(
        new CommentUpdatedEvent(
            comment.getId(),
            comment.getUser().getId(),
            comment.getContent()
        )
    );
    return commentMapper.toDto(comment, false);
  }

  @Transactional
  public void deleteComment(UUID commentId) {
    log.debug("댓글 삭제 요청 - commentId={}", commentId);

    Comment comment = findActiveComment(commentId);
    comment.markDeleted();
    newsArticleRepository.decrementCommentCountById(comment.getArticle().getId());
    log.info("댓글 삭제 성공 - commentId={}", commentId);
    eventPublisher.publishEvent(new CommentDeletedEvent(comment.getId(), comment.getUser().getId()));
  }

  @Transactional
  public void hardDeleteComment(UUID commentId) {
    log.debug("댓글 물리 삭제 요청 - commentId={}", commentId);

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
    log.info("댓글 물리 삭제 성공 - commentId={}", commentId);
    eventPublisher.publishEvent(new CommentDeletedEvent(comment.getId(), comment.getUser().getId()));
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
    log.debug(
        "댓글 목록 조회 요청 - articleId={}, orderBy={}, direction={}, cursor={}, after={}, limit={}, requestUserId={}",
        articleId,
        orderBy,
        direction,
        cursor,
        after,
        limit,
        requestUserId
    );

    CommentSearchCondition condition = createSearchCondition(
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
    CursorPageResponseCommentDto response = buildCommentPageResponse(condition, comments, totalElements);

    log.debug("댓글 목록 조회 완료 - articleId={}, size={}, totalElements={}, hasNext={}",
        articleId, response.size(), response.totalElements(), response.hasNext());
    return response;
  }

  @Transactional
  public CommentLikeDto likeComment(UUID commentId, UUID requestUserId) {
    log.debug("댓글 좋아요 요청 - commentId={}, requestUserId={}", commentId, requestUserId);

    Comment comment = findActiveCommentWithArticleAndUser(commentId);
    User user = findActiveUser(requestUserId);

    if (commentLikeRepository.existsByCommentIdAndUserId(commentId, requestUserId)) {
      log.warn("댓글 좋아요 실패 - commentId={}, requestUserId={}, reason=이미 좋아요한 댓글입니다.",
          commentId, requestUserId);
      throw new CommentLikeAlreadyExistsException();
    }

    CommentLike savedCommentLike = commentLikeRepository.save(CommentLike.create(comment, user));
    comment.increaseLikeCount();

    if (!comment.getUser().getId().equals(requestUserId)) {
      eventPublisher.publishEvent(CommentLikedEvent.of(requestUserId, commentId));
      log.debug("댓글 좋아요 이벤트 발행 완료 - commentId={}, requestUserId={}",
          commentId, requestUserId);
    }
    eventPublisher.publishEvent(CommentLikedActivityEvent.from(savedCommentLike));

    log.info("댓글 좋아요 성공 - commentId={}, requestUserId={}", commentId, requestUserId);
    return toCommentLikeDto(savedCommentLike);
  }

  @Transactional
  public void unlikeComment(UUID commentId, UUID requestUserId) {
    log.debug("댓글 좋아요 취소 요청 - commentId={}, requestUserId={}", commentId, requestUserId);

    Comment comment = findActiveComment(commentId);
    CommentLike commentLike = commentLikeRepository.findByCommentIdAndUserId(commentId, requestUserId)
        .orElseThrow(() -> {
          log.warn("댓글 좋아요 취소 실패 - commentId={}, requestUserId={}, reason=좋아요 이력이 없습니다.",
              commentId, requestUserId);
          return new CommentLikeNotFoundException();
        });

    comment.decreaseLikeCount();
    commentLikeRepository.delete(commentLike);
    log.info("댓글 좋아요 취소 성공 - commentId={}, requestUserId={}", commentId, requestUserId);
    eventPublisher.publishEvent(
        new CommentUnlikedEvent(
          requestUserId,
          commentLike.getId(),
          commentId
        )
    );
  }

  // 조회 파라미터를 검증하고 내부 검색 조건으로 변환한다.
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

  // 정렬 기준 문자열을 내부 enum으로 변환한다.
  private CommentOrderBy parseOrderBy(String orderBy) {
    if ("createdAt".equals(orderBy)) {
      return CommentOrderBy.CREATED_AT;
    }

    if ("likeCount".equals(orderBy)) {
      return CommentOrderBy.LIKE_COUNT;
    }

    throw invalidInput("orderBy", orderBy);
  }

  // 정렬 방향 문자열을 내부 enum으로 변환한다.
  private CommentDirection parseDirection(String direction) {
    if (direction == null || direction.isBlank() || "DESC".equalsIgnoreCase(direction)) {
      return CommentDirection.DESC;
    }

    throw invalidInput("direction", direction);
  }

  // 요청 커서를 정렬 기준에 맞는 값으로 해석한다.
  private CommentCursor parseCursor(CommentOrderBy orderBy, String cursor, Instant after) {
    String[] cursorValues = splitCursor(cursor);

    if (orderBy == CommentOrderBy.CREATED_AT) {
      return new CommentCursor(
          null,
          parseInstant(cursorValue(cursorValues, 0), "cursor", after),
          parseUuid(cursorValue(cursorValues, 1), "cursorId")
      );
    }

    return new CommentCursor(
        parseInteger(cursorValue(cursorValues, 0), "cursor"),
        parseInstant(resolveLikeCountCursorCreatedAt(cursorValues, after), "cursorCreatedAt", null),
        parseUuid(cursorValue(cursorValues, 2), "cursorId")
    );
  }

  // 정렬 조건에 맞는 댓글 목록 조회 쿼리를 선택한다.
  private List<Comment> findCommentPage(CommentSearchCondition condition) {
    Pageable pageable = PageRequest.of(0, condition.limit() + 1);

    if (condition.orderBy() == CommentOrderBy.CREATED_AT) {
      if (condition.cursor().createdAt() == null || condition.cursor().id() == null) {
        return commentRepository.findFirstActiveCommentsByCreatedAtDesc(
            condition.articleId(),
            pageable
        );
      }

      return commentRepository.findActiveCommentsByCreatedAtDesc(
          condition.articleId(),
          condition.cursor().createdAt(),
          condition.cursor().id(),
          pageable
      );
    }

    if (condition.cursor().likeCount() == null
        || condition.cursor().createdAt() == null
        || condition.cursor().id() == null) {
      return commentRepository.findFirstActiveCommentsByLikeCountDesc(
          condition.articleId(),
          pageable
      );
    }

    return commentRepository.findActiveCommentsByLikeCountDesc(
        condition.articleId(),
        condition.cursor().likeCount(),
        condition.cursor().createdAt(),
        condition.cursor().id(),
        pageable
    );
  }

  // 조회 결과를 커서 페이지 응답으로 조립한다.
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

  // 요청 사용자를 기준으로 likedByMe 값을 계산해 댓글 DTO로 변환한다.
  private List<CommentDto> toCommentDtos(List<Comment> comments, UUID requestUserId) {
    List<UUID> commentIds = comments.stream()
        .map(Comment::getId)
        .toList();

    Set<UUID> likedCommentIds = commentLikeRepository.findLikedCommentIds(requestUserId, commentIds);

    return comments.stream()
        .map(comment -> commentMapper.toDto(comment, likedCommentIds.contains(comment.getId())))
        .toList();
  }

  // 마지막 댓글을 기준으로 다음 페이지 커서를 만든다.
  private String resolveNextCursor(Comment comment, CommentOrderBy orderBy) {
    if (orderBy == CommentOrderBy.LIKE_COUNT) {
      return String.join(
          CURSOR_DELIMITER,
          String.valueOf(comment.getLikeCount()),
          comment.getCreatedAt().toString(),
          comment.getId().toString()
      );
    }

    return String.join(
        CURSOR_DELIMITER,
        comment.getCreatedAt().toString(),
        comment.getId().toString()
    );
  }

  // 좋아요 엔티티를 응답 DTO로 변환한다.
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

  // 등록 대상 기사가 활성 상태인지 확인한다.
  private NewsArticle findActiveArticle(UUID articleId) {
    return findArticle(articleId, () -> new ArticleNotFoundException(articleId));
  }

  // 조회 대상 기사가 읽을 수 있는 상태인지 확인한다.
  private NewsArticle findReadableArticle(UUID articleId) {
    return findArticle(articleId, () -> invalidInput("articleId", articleId));
  }

  // 기사 존재 여부와 삭제 상태를 함께 확인한다.
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

  // 활성 사용자만 반환한다.
  private User findActiveUser(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));

    if (user.isDeleted()) {
      throw new DeletedUserException(userId);
    }

    return user;
  }

  // 활성 댓글만 반환한다.
  private Comment findActiveComment(UUID commentId) {
    Comment comment = findComment(commentId);

    if (comment.isDeleted()) {
      throw new DeletedCommentException(commentId);
    }

    return comment;
  }

  // event 객체 매핑을 위해 연관된 객체 같이 조회
  private Comment findActiveCommentWithArticleAndUser(UUID commentId) {
    // event 객체 매핑을 위해 연관된 객체 같이 조회
    Comment comment = commentRepository.findByIdWithArticleAndUser(commentId)
        .orElseThrow(() -> new CommentNotFoundException(commentId));

    if (comment.isDeleted()) {
      throw new DeletedCommentException(commentId);
    }
    return comment;
  }

  // 삭제 여부와 무관하게 댓글 존재 여부만 확인한다.
  private Comment findComment(UUID commentId) {
    return commentRepository.findById(commentId)
        .orElseThrow(() -> new CommentNotFoundException(commentId));
  }

  // 요청 사용자가 댓글 작성자인지 확인한다.
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

  // 페이지 크기 파라미터를 검증한다.
  private void validateLimit(int limit) {
    if (limit < 1) {
      throw invalidInput("limit", limit);
    }
  }

  // 커서 문자열을 구분자로 분리한다.
  private String[] splitCursor(String cursor) {
    if (cursor == null || cursor.isBlank()) {
      return new String[0];
    }

    return cursor.split(Pattern.quote(CURSOR_DELIMITER));
  }

  // 분리된 커서 배열에서 필요한 위치의 값을 꺼낸다.
  private String cursorValue(String[] cursorValues, int index) {
    if (cursorValues.length <= index) {
      return null;
    }

    return cursorValues[index];
  }

  // 문자열 시간을 Instant로 변환한다.
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

  // 문자열 숫자를 Integer로 변환한다.
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

  // 문자열 UUID를 UUID 객체로 변환한다.
  private UUID parseUuid(String value, String field) {
    if (value == null || value.isBlank()) {
      return null;
    }

    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      throw invalidInput(field, value);
    }
  }

  // 좋아요순 커서에서 createdAt 값을 보정해 반환한다.
  private String resolveLikeCountCursorCreatedAt(String[] cursorValues, Instant after) {
    String cursorCreatedAt = cursorValue(cursorValues, 1);

    if (cursorCreatedAt != null && !cursorCreatedAt.isBlank()) {
      return cursorCreatedAt;
    }

    if (cursorValues.length > 0 && cursorValues[0] != null && !cursorValues[0].isBlank()) {
      if (after == null) {
        throw invalidInput("cursor", String.join(CURSOR_DELIMITER, cursorValues));
      }

      return after.toString();
    }

    return after == null ? null : after.toString();
  }

  // 공통 입력값 오류 예외를 생성한다.
  private BusinessException invalidInput(String field, Object value) {
    return new BusinessException(
        ErrorCode.INVALID_INPUT_VALUE,
        Map.of(field, String.valueOf(value))
    );
  }
}
