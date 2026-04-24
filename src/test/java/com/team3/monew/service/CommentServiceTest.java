package com.team3.monew.service;

import com.team3.monew.dto.comment.CommentDto;
import com.team3.monew.dto.comment.CommentLikeDto;
import com.team3.monew.dto.comment.CommentRegisterRequest;
import com.team3.monew.dto.comment.CommentUpdateRequest;
import com.team3.monew.entity.Comment;
import com.team3.monew.entity.CommentLike;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.NewsSource;
import com.team3.monew.entity.User;
import com.team3.monew.entity.enums.DeleteStatus;
import com.team3.monew.entity.enums.NotificationResourceType;
import com.team3.monew.event.CommentLikedEvent;
import com.team3.monew.exception.article.ArticleNotFoundException;
import com.team3.monew.exception.article.DeletedArticleException;
import com.team3.monew.exception.comment.CommentNotFoundException;
import com.team3.monew.exception.comment.DeletedCommentException;
import com.team3.monew.exception.comment.UnauthorizedCommentUpdateException;
import com.team3.monew.exception.user.DeletedUserException;
import com.team3.monew.exception.user.UserNotFoundException;
import com.team3.monew.global.exception.BusinessException;
import com.team3.monew.mapper.CommentMapper;
import com.team3.monew.repository.CommentLikeRepository;
import com.team3.monew.repository.CommentRepository;
import com.team3.monew.repository.NewsArticleRepository;
import com.team3.monew.repository.NotificationRepository;
import com.team3.monew.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private NewsArticleRepository newsArticleRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CommentService commentService;

    private UUID articleId;
    private UUID userId;
    private UUID requestUserId;
    private UUID commentId;
    private String content;
    private String updatedContent;
    private CommentRegisterRequest request;
    private CommentUpdateRequest updateRequest;
    private NewsArticle article;
    private User user;

    @BeforeEach
    void setUp() {
        articleId = UUID.randomUUID();
        userId = UUID.randomUUID();
        requestUserId = UUID.randomUUID();
        commentId = UUID.randomUUID();
        content = "댓글 내용입니다.";
        updatedContent = "수정된 댓글 내용입니다.";
        request = new CommentRegisterRequest(articleId, userId, content);
        updateRequest = new CommentUpdateRequest(updatedContent);
        article = NewsArticle.create(
                mock(NewsSource.class),
                "https://news.example.com/articles/1",
                "테스트 기사",
                Instant.parse("2026-04-17T00:00:00Z"),
                "테스트 기사 요약"
        );
        user = User.create("user@example.com", "테스터", "encoded-password");
        assignId(article, articleId);
        assignId(user, userId);
    }

    @Nested
    @DisplayName("댓글 등록 기능을 검증한다.")
    class RegisterComment {

        @Test
        @DisplayName("존재하는 기사와 사용자로 댓글을 등록하면 댓글 등록 결과를 반환한다.")
        void shouldRegisterComment_whenArticleAndUserExist() {
            // given
            Comment savedComment = Comment.create(article, user, content);
            CommentDto expected = new CommentDto(
                    UUID.randomUUID(),
                    articleId,
                    userId,
                    user.getNickname(),
                    content,
                    0L,
                    false,
                    Instant.parse("2026-04-17T00:00:01Z")
            );

            given(newsArticleRepository.findById(articleId)).willReturn(Optional.of(article));
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(commentRepository.save(any(Comment.class))).willReturn(savedComment);
            given(commentMapper.toDto(savedComment, false)).willReturn(expected);

            // when
            CommentDto actual = commentService.registerComment(request);

            // then
            assertThat(actual).isEqualTo(expected);
            then(newsArticleRepository).should().findById(articleId);
            then(userRepository).should().findById(userId);
            then(commentRepository).should().save(argThat(comment ->
                    comment.getArticle() == article
                            && comment.getUser() == user
                            && comment.getContent().equals(content)
                            && comment.getLikeCount() == 0
            ));
            then(newsArticleRepository).should().incrementCommentCountById(articleId);
            then(commentMapper).should().toDto(savedComment, false);
        }

        @Test
        @DisplayName("존재하지 않는 기사에 댓글을 등록하면 기사 없음 예외가 발생한다.")
        void shouldThrowArticleNotFoundException_whenArticleDoesNotExist() {
            // given
            given(newsArticleRepository.findById(articleId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.registerComment(request))
                    .isInstanceOf(ArticleNotFoundException.class);

            then(newsArticleRepository).should().findById(articleId);
            then(commentRepository).shouldHaveNoInteractions();
            then(commentMapper).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 댓글을 등록하면 사용자 없음 예외가 발생한다.")
        void shouldThrowUserNotFoundException_whenUserDoesNotExist() {
            // given
            given(newsArticleRepository.findById(articleId)).willReturn(Optional.of(article));
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.registerComment(request))
                    .isInstanceOf(UserNotFoundException.class);

            then(newsArticleRepository).should().findById(articleId);
            then(userRepository).should().findById(userId);
            then(commentRepository).shouldHaveNoInteractions();
            then(commentMapper).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("삭제된 기사에 댓글을 등록하면 삭제된 기사 예외가 발생한다.")
        void shouldThrowDeletedArticleException_whenArticleIsDeleted() {
            // given
            markDeleted(article);
            given(newsArticleRepository.findById(articleId)).willReturn(Optional.of(article));

            // when & then
            assertThatThrownBy(() -> commentService.registerComment(request))
                    .isInstanceOf(DeletedArticleException.class);

            then(newsArticleRepository).should().findById(articleId);
            then(commentRepository).shouldHaveNoInteractions();
            then(commentMapper).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("삭제된 사용자로 댓글을 등록하면 삭제된 사용자 예외가 발생한다.")
        void shouldThrowDeletedUserException_whenUserIsDeleted() {
            // given
            markDeleted(user);
            given(newsArticleRepository.findById(articleId)).willReturn(Optional.of(article));
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // when & then
            assertThatThrownBy(() -> commentService.registerComment(request))
                    .isInstanceOf(DeletedUserException.class);

            then(newsArticleRepository).should().findById(articleId);
            then(userRepository).should().findById(userId);
            then(commentRepository).shouldHaveNoInteractions();
            then(commentMapper).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("댓글 수정 기능을 검증한다.")
    class UpdateComment {

        @Test
        @DisplayName("작성자가 자신의 댓글을 수정하면 수정된 댓글 결과를 반환한다.")
        void shouldUpdateComment_whenCommentAuthorRequests() {
            // given
            Comment comment = Comment.create(article, user, content);
            assignId(comment, commentId);
            CommentDto expected = new CommentDto(
                    commentId,
                    articleId,
                    userId,
                    user.getNickname(),
                    updatedContent,
                    0L,
                    false,
                    Instant.parse("2026-04-17T00:00:01Z")
            );

            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(commentMapper.toDto(comment, false)).willReturn(expected);

            // when
            CommentDto actual = commentService.updateComment(commentId, userId, updateRequest);

            // then
            assertThat(actual).isEqualTo(expected);
            assertThat(comment.getContent()).isEqualTo(updatedContent);
            then(commentRepository).should().findById(commentId);
            then(commentMapper).should().toDto(comment, false);
        }

        @Test
        @DisplayName("존재하지 않는 댓글을 수정하면 댓글 없음 예외가 발생한다.")
        void shouldThrowCommentNotFoundException_whenCommentDoesNotExist() {
            // given
            given(commentRepository.findById(commentId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.updateComment(commentId, userId, updateRequest))
                    .isInstanceOf(CommentNotFoundException.class);

            then(commentRepository).should().findById(commentId);
            then(commentMapper).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("삭제된 댓글을 수정하면 삭제된 댓글 예외가 발생한다.")
        void shouldThrowDeletedCommentException_whenCommentIsDeleted() {
            // given
            Comment comment = Comment.create(article, user, content);
            assignId(comment, commentId);
            markDeleted(comment);
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

            // when & then
            assertThatThrownBy(() -> commentService.updateComment(commentId, userId, updateRequest))
                    .isInstanceOf(DeletedCommentException.class);

            assertThat(comment.getContent()).isEqualTo(content);
            then(commentRepository).should().findById(commentId);
            then(commentMapper).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("작성자가 아닌 사용자가 댓글을 수정하면 댓글 수정 권한 없음 예외가 발생한다.")
        void shouldThrowUnauthorizedCommentUpdateException_whenUserIsNotAuthor() {
            // given
            UUID otherUserId = UUID.randomUUID();
            CommentUpdateRequest otherUserRequest = new CommentUpdateRequest(updatedContent);
            Comment comment = Comment.create(article, user, content);
            assignId(comment, commentId);
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

            // when & then
            assertThatThrownBy(() -> commentService.updateComment(commentId, otherUserId, otherUserRequest))
                    .isInstanceOf(UnauthorizedCommentUpdateException.class);

            assertThat(comment.getContent()).isEqualTo(content);
            then(commentRepository).should().findById(commentId);
            then(commentMapper).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("삭제된 작성자가 댓글을 수정하면 삭제된 사용자 예외가 발생한다.")
        void shouldThrowDeletedUserException_whenAuthorIsDeleted() {
            // given
            markDeleted(user);
            Comment comment = Comment.create(article, user, content);
            assignId(comment, commentId);
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

            // when & then
            assertThatThrownBy(() -> commentService.updateComment(commentId, userId, updateRequest))
                    .isInstanceOf(DeletedUserException.class);

            assertThat(comment.getContent()).isEqualTo(content);
            then(commentRepository).should().findById(commentId);
            then(commentMapper).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("댓글 삭제 기능을 검증한다.")
    class DeleteComment {

        @Test
        @DisplayName("존재하는 댓글을 삭제하면 댓글이 삭제 상태로 변경되고 기사 댓글 수가 감소한다.")
        void shouldDeleteComment_whenCommentExists() {
            // given
            Comment comment = Comment.create(article, user, content);
            assignId(comment, commentId);
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

            // when
            commentService.deleteComment(commentId);

            // then
            assertThat(comment.isDeleted()).isTrue();
            assertThat(comment.getDeletedAt()).isNotNull();
            then(newsArticleRepository).should().decrementCommentCountById(articleId);
        }

        @Test
        @DisplayName("존재하지 않는 댓글을 삭제하면 댓글 없음 예외가 발생한다.")
        void shouldThrowCommentNotFoundException_whenDeleteCommentDoesNotExist() {
            // given
            given(commentRepository.findById(commentId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.deleteComment(commentId))
                    .isInstanceOf(CommentNotFoundException.class);

            then(newsArticleRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("이미 삭제된 댓글을 삭제하면 삭제된 댓글 예외가 발생한다.")
        void shouldThrowDeletedCommentException_whenDeleteCommentIsAlreadyDeleted() {
            // given
            Comment comment = Comment.create(article, user, content);
            assignId(comment, commentId);
            markDeleted(comment);
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

            // when & then
            assertThatThrownBy(() -> commentService.deleteComment(commentId))
                    .isInstanceOf(DeletedCommentException.class);

            then(newsArticleRepository).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("댓글 물리 삭제 기능을 검증한다.")
    class HardDeleteComment {

        @Test
        @DisplayName("활성 댓글을 물리 삭제하면 관련 데이터가 삭제되고 기사 댓글 수가 감소한다.")
        void shouldHardDeleteCommentAndDecreaseCommentCount_whenActiveCommentExists() {
            // given
            Comment comment = Comment.create(article, user, content);
            assignId(comment, commentId);
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

            // when
            commentService.hardDeleteComment(commentId);

            // then
            then(newsArticleRepository).should().decrementCommentCountById(articleId);
            then(commentLikeRepository).should().deleteByCommentId(commentId);
            then(notificationRepository).should()
                    .deleteByResourceTypeAndResourceId(NotificationResourceType.COMMENT, commentId);
            then(commentRepository).should().delete(comment);
        }

        @Test
        @DisplayName("이미 삭제된 댓글을 물리 삭제하면 기사 댓글 수 감소 없이 관련 데이터만 삭제한다.")
        void shouldHardDeleteCommentWithoutDecreasingCommentCount_whenDeletedCommentExists() {
            // given
            Comment comment = Comment.create(article, user, content);
            assignId(comment, commentId);
            markDeleted(comment);
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

            // when
            commentService.hardDeleteComment(commentId);

            // then
            then(newsArticleRepository).shouldHaveNoInteractions();
            then(commentLikeRepository).should().deleteByCommentId(commentId);
            then(notificationRepository).should()
                    .deleteByResourceTypeAndResourceId(NotificationResourceType.COMMENT, commentId);
            then(commentRepository).should().delete(comment);
        }

        @Test
        @DisplayName("존재하지 않는 댓글을 물리 삭제하면 댓글 없음 예외가 발생한다.")
        void shouldThrowCommentNotFoundException_whenHardDeleteCommentDoesNotExist() {
            // given
            given(commentRepository.findById(commentId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.hardDeleteComment(commentId))
                    .isInstanceOf(CommentNotFoundException.class);

            then(newsArticleRepository).shouldHaveNoInteractions();
            then(commentLikeRepository).shouldHaveNoInteractions();
            then(notificationRepository).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("댓글 조회 기능을 검증한다.")
    class FindAllComment {

        @Test
        @DisplayName("댓글 목록을 조회하면 페이지 응답과 요청자의 좋아요 여부를 반환한다.")
        void shouldFindCommentsWithLikedByMe_whenRequestIsValid() {
            // given
            int limit = 2;
            Comment firstComment = createComment("첫 번째 댓글입니다.", "2026-04-17T00:00:03Z", 7);
            Comment secondComment = createComment("두 번째 댓글입니다.", "2026-04-17T00:00:02Z", 5);
            Comment thirdComment = createComment("세 번째 댓글입니다.", "2026-04-17T00:00:01Z", 3);
            List<Comment> pageComments = List.of(firstComment, secondComment);
            CommentDto firstDto = givenCommentDto(firstComment, false);
            CommentDto secondDto = givenCommentDto(secondComment, true);

            givenActiveComments("likeCount", null, null, limit,
                    List.of(firstComment, secondComment, thirdComment), 3L);
            givenLikedComments(pageComments, Set.of(secondComment.getId()));

            // when
            var actual = commentService.findAll(articleId, "likeCount", null, null, limit, requestUserId);

            // then
            assertThat(actual.content()).containsExactly(firstDto, secondDto);
            assertThat(actual.nextCursor()).isEqualTo(cursorOf(secondComment, "likeCount"));
            assertThat(actual.nextAfter()).isEqualTo(secondComment.getCreatedAt());
            assertThat(actual.size()).isEqualTo(limit);
            assertThat(actual.totalElements()).isEqualTo(3L);
            assertThat(actual.hasNext()).isTrue();
        }

        @Test
        @DisplayName("조회 결과가 없으면 빈 페이지 응답을 반환한다.")
        void shouldReturnEmptyPage_whenCommentsDoNotExist() {
            // given
            int limit = 10;
            givenActiveComments("createdAt", null, null, limit, List.of(), 0L);

            // when
            var actual = commentService.findAll(articleId, "createdAt", null, null, limit, requestUserId);

            // then
            assertThat(actual.content()).isEmpty();
            assertThat(actual.nextCursor()).isNull();
            assertThat(actual.nextAfter()).isNull();
            assertThat(actual.size()).isZero();
            assertThat(actual.totalElements()).isZero();
            assertThat(actual.hasNext()).isFalse();
            then(commentLikeRepository).shouldHaveNoInteractions();
            then(commentMapper).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("커서가 있으면 다음 페이지 댓글 목록을 조회한다.")
        void shouldFindNextPageComments_whenCursorExists() {
            // given
            int limit = 1;
            Comment cursorComment = createComment("이전 페이지 마지막 댓글입니다.", "2026-04-17T00:00:02Z", 5);
            String cursor = cursorOf(cursorComment, "likeCount");
            Comment nextComment = createComment("다음 페이지 댓글입니다.", "2026-04-17T00:00:01Z", 4);
            Comment lookAheadComment = createComment("다음 페이지 존재 확인용 댓글입니다.", "2026-04-17T00:00:00Z", 2);
            List<Comment> pageComments = List.of(nextComment);
            CommentDto nextDto = givenCommentDto(nextComment, false);

            givenActiveComments("likeCount", cursor, null, limit,
                    List.of(nextComment, lookAheadComment), 4L);
            givenLikedComments(pageComments, Set.of());

            // when
            var actual = commentService.findAll(articleId, "likeCount", cursor, null, limit, requestUserId);

            // then
            assertThat(actual.content()).containsExactly(nextDto);
            assertThat(actual.nextCursor()).isEqualTo(cursorOf(nextComment, "likeCount"));
            assertThat(actual.nextAfter()).isEqualTo(nextComment.getCreatedAt());
            assertThat(actual.size()).isEqualTo(limit);
            assertThat(actual.totalElements()).isEqualTo(4L);
            assertThat(actual.hasNext()).isTrue();
        }

        @Test
        @DisplayName("limit가 1보다 작으면 댓글 목록 조회에 실패한다.")
        void shouldThrowBusinessException_whenLimitIsLessThanOne() {
            // when & then
            assertThatThrownBy(() ->
                    commentService.findAll(articleId, "createdAt", null, null, 0, requestUserId)
            ).isInstanceOf(BusinessException.class);

            then(commentRepository).shouldHaveNoInteractions();
            then(commentLikeRepository).shouldHaveNoInteractions();
            then(commentMapper).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("정렬 기준이 유효하지 않으면 댓글 목록 조회에 실패한다.")
        void shouldThrowBusinessException_whenOrderByIsInvalid() {
            // when & then
            assertThatThrownBy(() ->
                    commentService.findAll(articleId, "invalid", null, null, 10, requestUserId)
            ).isInstanceOf(BusinessException.class);

            then(commentRepository).shouldHaveNoInteractions();
            then(commentLikeRepository).shouldHaveNoInteractions();
            then(commentMapper).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("댓글 좋아요 등록 기능을 검증한다.")
    class LikeComment {

        @Test
        @DisplayName("다른 사용자가 활성 댓글에 좋아요를 누르면 좋아요를 저장하고 댓글 좋아요 수를 증가시키며 알림 이벤트를 발행한다.")
        void shouldLikeCommentAndPublishEvent_whenOtherUserLikesActiveComment() {
            // given
            User liker = createUser(requestUserId, "liker@example.com", "좋아요사용자");
            Comment comment = createComment(content, "2026-04-17T00:00:01Z", 0);
            assignId(comment, commentId);
            CommentLike savedLike = createCommentLike(comment, liker);
            CommentLikeDto expected = commentLikeDto(savedLike, 1L);

            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(userRepository.findById(requestUserId)).willReturn(Optional.of(liker));
            given(commentLikeRepository.existsByCommentIdAndUserId(commentId, requestUserId))
                    .willReturn(false);
            given(commentLikeRepository.save(any(CommentLike.class))).willReturn(savedLike);

            // when
            CommentLikeDto actual = commentService.likeComment(commentId, requestUserId);

            // then
            assertThat(actual).isEqualTo(expected);
            assertThat(comment.getLikeCount()).isEqualTo(1);
            then(commentLikeRepository).should().save(argThat(commentLike ->
                    commentLike.getComment() == comment && commentLike.getUser() == liker
            ));
            then(eventPublisher).should().publishEvent(new CommentLikedEvent(requestUserId, commentId));
        }

        @Test
        @DisplayName("작성자가 자신의 댓글에 좋아요를 누르면 좋아요는 저장하지만 알림 이벤트는 발행하지 않는다.")
        void shouldLikeCommentWithoutPublishingEvent_whenAuthorLikesOwnComment() {
            // given
            Comment comment = createComment(content, "2026-04-17T00:00:01Z", 0);
            assignId(comment, commentId);
            CommentLike savedLike = createCommentLike(comment, user);
            CommentLikeDto expected = commentLikeDto(savedLike, 1L);

            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(commentLikeRepository.existsByCommentIdAndUserId(commentId, userId))
                    .willReturn(false);
            given(commentLikeRepository.save(any(CommentLike.class))).willReturn(savedLike);

            // when
            CommentLikeDto actual = commentService.likeComment(commentId, userId);

            // then
            assertThat(actual).isEqualTo(expected);
            assertThat(comment.getLikeCount()).isEqualTo(1);
            then(commentLikeRepository).should().save(any(CommentLike.class));
            then(eventPublisher).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("이미 좋아요를 누른 댓글에 다시 좋아요를 누르면 좋아요 등록에 실패한다.")
        void shouldThrowBusinessException_whenCommentLikeAlreadyExists() {
            // given
            User liker = createUser(requestUserId, "liker@example.com", "좋아요사용자");
            Comment comment = createComment(content, "2026-04-17T00:00:01Z", 1);
            assignId(comment, commentId);

            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(userRepository.findById(requestUserId)).willReturn(Optional.of(liker));
            given(commentLikeRepository.existsByCommentIdAndUserId(commentId, requestUserId))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> commentService.likeComment(commentId, requestUserId))
                    .isInstanceOf(BusinessException.class);

            assertThat(comment.getLikeCount()).isEqualTo(1);
            then(commentLikeRepository).should().existsByCommentIdAndUserId(commentId, requestUserId);
            then(commentLikeRepository).shouldHaveNoMoreInteractions();
            then(eventPublisher).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("존재하지 않는 댓글에 좋아요를 누르면 댓글 없음 예외가 발생한다.")
        void shouldThrowCommentNotFoundException_whenLikeCommentDoesNotExist() {
            // given
            given(commentRepository.findById(commentId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.likeComment(commentId, requestUserId))
                    .isInstanceOf(CommentNotFoundException.class);

            then(userRepository).shouldHaveNoInteractions();
            then(commentLikeRepository).shouldHaveNoInteractions();
            then(eventPublisher).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("삭제된 댓글에 좋아요를 누르면 삭제된 댓글 예외가 발생한다.")
        void shouldThrowDeletedCommentException_whenLikeDeletedComment() {
            // given
            Comment comment = createComment(content, "2026-04-17T00:00:01Z", 0);
            assignId(comment, commentId);
            markDeleted(comment);
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

            // when & then
            assertThatThrownBy(() -> commentService.likeComment(commentId, requestUserId))
                    .isInstanceOf(DeletedCommentException.class);

            then(userRepository).shouldHaveNoInteractions();
            then(commentLikeRepository).shouldHaveNoInteractions();
            then(eventPublisher).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("삭제된 사용자가 좋아요를 누르면 삭제된 사용자 예외가 발생한다.")
        void shouldThrowDeletedUserException_whenDeletedUserLikesComment() {
            // given
            User liker = createUser(requestUserId, "deleted@example.com", "삭제사용자");
            markDeleted(liker);
            Comment comment = createComment(content, "2026-04-17T00:00:01Z", 0);
            assignId(comment, commentId);

            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(userRepository.findById(requestUserId)).willReturn(Optional.of(liker));

            // when & then
            assertThatThrownBy(() -> commentService.likeComment(commentId, requestUserId))
                    .isInstanceOf(DeletedUserException.class);

            assertThat(comment.getLikeCount()).isZero();
            then(commentLikeRepository).shouldHaveNoInteractions();
            then(eventPublisher).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("댓글 좋아요 취소 기능을 검증한다.")
    class UnlikeComment {

        @Test
        @DisplayName("좋아요한 댓글의 좋아요를 취소하면 좋아요를 삭제하고 댓글 좋아요 수를 감소시킨다.")
        void shouldUnlikeComment_whenCommentLikeExists() {
            // given
            User liker = createUser(requestUserId, "liker@example.com", "좋아요사용자");
            Comment comment = createComment(content, "2026-04-17T00:00:01Z", 1);
            assignId(comment, commentId);
            CommentLike commentLike = createCommentLike(comment, liker);

            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(commentLikeRepository.findByCommentIdAndUserId(commentId, requestUserId))
                    .willReturn(Optional.of(commentLike));

            // when
            commentService.unlikeComment(commentId, requestUserId);

            // then
            assertThat(comment.getLikeCount()).isZero();
            then(commentLikeRepository).should().delete(commentLike);
            then(eventPublisher).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("존재하지 않는 댓글의 좋아요를 취소하면 댓글 없음 예외가 발생한다.")
        void shouldThrowCommentNotFoundException_whenUnlikeCommentDoesNotExist() {
            // given
            given(commentRepository.findById(commentId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.unlikeComment(commentId, requestUserId))
                    .isInstanceOf(CommentNotFoundException.class);

            then(commentLikeRepository).shouldHaveNoInteractions();
            then(eventPublisher).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("삭제된 댓글의 좋아요를 취소하면 삭제된 댓글 예외가 발생한다.")
        void shouldThrowDeletedCommentException_whenUnlikeDeletedComment() {
            // given
            Comment comment = createComment(content, "2026-04-17T00:00:01Z", 1);
            assignId(comment, commentId);
            markDeleted(comment);
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

            // when & then
            assertThatThrownBy(() -> commentService.unlikeComment(commentId, requestUserId))
                    .isInstanceOf(DeletedCommentException.class);

            assertThat(comment.getLikeCount()).isEqualTo(1);
            then(commentLikeRepository).shouldHaveNoInteractions();
            then(eventPublisher).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("좋아요하지 않은 댓글의 좋아요를 취소하면 좋아요 취소에 실패한다.")
        void shouldThrowBusinessException_whenCommentLikeDoesNotExist() {
            // given
            Comment comment = createComment(content, "2026-04-17T00:00:01Z", 1);
            assignId(comment, commentId);

            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(commentLikeRepository.findByCommentIdAndUserId(commentId, requestUserId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.unlikeComment(commentId, requestUserId))
                    .isInstanceOf(BusinessException.class);

            assertThat(comment.getLikeCount()).isEqualTo(1);
            then(commentLikeRepository).should().findByCommentIdAndUserId(commentId, requestUserId);
            then(eventPublisher).shouldHaveNoInteractions();
        }
    }

    private void assignId(Object entity, UUID id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }

    private void markDeleted(Object entity) {
        ReflectionTestUtils.setField(entity, "deleteStatus", DeleteStatus.DELETED);
        ReflectionTestUtils.setField(entity, "deletedAt", Instant.parse("2026-04-17T00:00:00Z"));
    }

    private void givenActiveComments(String orderBy, String cursor, Instant after, int limit,
                                     List<Comment> comments, long totalElements) {
        PageRequest pageable = PageRequest.of(0, limit + 1);
        if ("likeCount".equals(orderBy)) {
            given(commentRepository.findActiveCommentsByLikeCountDesc(
                    articleId,
                    cursorLikeCount(cursor),
                    likeCountCursorCreatedAt(cursor, after),
                    pageable
            )).willReturn(comments);
        } else {
            given(commentRepository.findActiveCommentsByCreatedAtDesc(
                    articleId,
                    createdAtCursor(cursor),
                    pageable
            )).willReturn(comments);
        }
        given(commentRepository.countActiveComments(articleId)).willReturn(totalElements);
    }

    private void givenLikedComments(List<Comment> comments, Set<UUID> likedCommentIds) {
        given(commentLikeRepository.findLikedCommentIds(requestUserId, commentIdsOf(comments)))
                .willReturn(likedCommentIds);
    }

    private CommentDto givenCommentDto(Comment comment, boolean likedByMe) {
        CommentDto dto = new CommentDto(comment.getId(), articleId, userId, user.getNickname(),
                comment.getContent(), (long) comment.getLikeCount(), likedByMe, comment.getCreatedAt());
        given(commentMapper.toDto(comment, likedByMe)).willReturn(dto);
        return dto;
    }

    private List<UUID> commentIdsOf(List<Comment> comments) {
        return comments.stream().map(Comment::getId).toList();
    }

    private Comment createComment(String content, String createdAt, int likeCount) {
        Comment comment = Comment.create(article, user, content);
        assignId(comment, UUID.randomUUID());
        ReflectionTestUtils.setField(comment, "createdAt", Instant.parse(createdAt));
        ReflectionTestUtils.setField(comment, "likeCount", likeCount);
        return comment;
    }

    private User createUser(UUID id, String email, String nickname) {
        User user = User.create(email, nickname, "encoded-password");
        assignId(user, id);
        return user;
    }

    private CommentLike createCommentLike(Comment comment, User user) {
        CommentLike commentLike = CommentLike.create(comment, user);
        assignId(commentLike, UUID.randomUUID());
        ReflectionTestUtils.setField(commentLike, "createdAt", Instant.parse("2026-04-17T00:00:02Z"));
        return commentLike;
    }

    private CommentLikeDto commentLikeDto(CommentLike commentLike, long commentLikeCount) {
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
                commentLikeCount,
                comment.getCreatedAt()
        );
    }

    private String cursorOf(Comment comment, String orderBy) {
        if ("likeCount".equals(orderBy)) {
            return comment.getLikeCount() + "|" + comment.getCreatedAt();
        }
        return comment.getCreatedAt().toString();
    }

    private Integer cursorLikeCount(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        return Integer.valueOf(cursor.split("\\|")[0]);
    }

    private Instant likeCountCursorCreatedAt(String cursor, Instant after) {
        if (cursor == null || cursor.isBlank()) {
            return after;
        }
        String[] cursorValues = cursor.split("\\|");
        if (cursorValues.length < 2 || cursorValues[1].isBlank()) {
            return after;
        }
        return Instant.parse(cursorValues[1]);
    }

    private Instant createdAtCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        return Instant.parse(cursor);
    }
}
