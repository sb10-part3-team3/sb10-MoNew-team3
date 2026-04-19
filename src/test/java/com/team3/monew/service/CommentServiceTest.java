package com.team3.monew.service;

import com.team3.monew.dto.comment.CommentDto;
import com.team3.monew.dto.comment.CommentRegisterRequest;
import com.team3.monew.dto.comment.CommentUpdateRequest;
import com.team3.monew.entity.Comment;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.NewsSource;
import com.team3.monew.entity.User;
import com.team3.monew.entity.enums.DeleteStatus;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
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
    private CommentMapper commentMapper;

    @Mock
    private NewsArticleRepository newsArticleRepository;

    @InjectMocks
    private CommentService commentService;

    private UUID articleId;
    private UUID userId;
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
        commentId = UUID.randomUUID();
        content = "댓글 내용입니다.";
        updatedContent = "수정된 댓글 내용입니다.";
        request = new CommentRegisterRequest(articleId, content);
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
            CommentDto actual = commentService.registerComment(request, userId);

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
            assertThatThrownBy(() -> commentService.registerComment(request, userId))
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
            assertThatThrownBy(() -> commentService.registerComment(request, userId))
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
            assertThatThrownBy(() -> commentService.registerComment(request, userId))
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
            assertThatThrownBy(() -> commentService.registerComment(request, userId))
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
            CommentDto actual = commentService.updateComment(commentId, updateRequest, userId);

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
            assertThatThrownBy(() -> commentService.updateComment(commentId, updateRequest, userId))
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
            assertThatThrownBy(() -> commentService.updateComment(commentId, updateRequest, userId))
                    .isInstanceOf(DeletedCommentException.class);

            assertThat(comment.getContent()).isEqualTo(content);
            then(commentRepository).should().findById(commentId);
            then(commentMapper).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("작성자가 아닌 사용자가 댓글을 수정하면 댓글 수정 권한 없음 예외가 발생한다.")
        void shouldThrowUnauthorizedCommentException_whenUserIsNotAuthor() {
            // given
            UUID otherUserId = UUID.randomUUID();
            Comment comment = Comment.create(article, user, content);
            assignId(comment, commentId);
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

            // when & then
            assertThatThrownBy(() -> commentService.updateComment(commentId, updateRequest, otherUserId))
                    .isInstanceOf(UnauthorizedCommentException.class);

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
            assertThatThrownBy(() -> commentService.updateComment(commentId, updateRequest, userId))
                    .isInstanceOf(DeletedUserException.class);

            assertThat(comment.getContent()).isEqualTo(content);
            then(commentRepository).should().findById(commentId);
            then(commentMapper).shouldHaveNoInteractions();
        }
    }

    private void assignId(Object entity, UUID id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }

    private void markDeleted(Object entity) {
        ReflectionTestUtils.setField(entity, "deleteStatus", DeleteStatus.DELETED);
        ReflectionTestUtils.setField(entity, "deletedAt", Instant.parse("2026-04-17T00:00:00Z"));
    }
}
