package com.team3.monew.service;

import com.team3.monew.dto.data.CommentDto;
import com.team3.monew.dto.request.CommentRegisterRequest;
import com.team3.monew.entity.Comment;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.NewsSource;
import com.team3.monew.entity.User;
import com.team3.monew.entity.enums.DeleteStatus;
import com.team3.monew.global.exception.BusinessException;
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
import static org.mockito.Mockito.mock;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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
    private String content;
    private CommentRegisterRequest request;
    private NewsArticle article;
    private User user;

    @BeforeEach
    void setUp() {
        articleId = UUID.randomUUID();
        userId = UUID.randomUUID();
        content = "댓글 내용입니다.";
        request = new CommentRegisterRequest(articleId, userId, content);
        article = NewsArticle.create(
                mock(NewsSource.class),
                "https://news.example.com/articles/1",
                "테스트 기사",
                Instant.parse("2026-04-17T00:00:00Z"),
                "테스트 기사 요약"
        );
        user = User.create("user@example.com", "테스터", "encoded-password");
    }

    @Nested
    @DisplayName("댓글 등록은")
    class RegisterComment {

        @Test
        @DisplayName("존재하는 기사와 사용자로 댓글을 성공적으로 등록한다.")
        void registerComment_Success() {
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
            then(commentMapper).should().toDto(savedComment, false);
            then(newsArticleRepository).shouldHaveNoMoreInteractions();
            then(userRepository).shouldHaveNoMoreInteractions();
            then(commentRepository).shouldHaveNoMoreInteractions();
            then(commentMapper).shouldHaveNoMoreInteractions();
        }

        @Test
        @DisplayName("존재하지 않는 기사에는 등록할 수 없다.")
        void registerComment_NotExistArticle_ThrowsException() {
            // given
            given(newsArticleRepository.findById(articleId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.registerComment(request))
                    .isInstanceOf(BusinessException.class);

            then(newsArticleRepository).should().findById(articleId);
            then(newsArticleRepository).shouldHaveNoMoreInteractions();
            then(userRepository).shouldHaveNoInteractions();
            then(commentRepository).shouldHaveNoInteractions();
            then(commentMapper).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("존재하지 않는 사용자로는 등록할 수 없다.")
        void registerComment_NotExistUser_ThrowsException() {
            // given
            given(newsArticleRepository.findById(articleId)).willReturn(Optional.of(article));
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.registerComment(request))
                    .isInstanceOf(BusinessException.class);

            then(newsArticleRepository).should().findById(articleId);
            then(userRepository).should().findById(userId);
            then(newsArticleRepository).shouldHaveNoMoreInteractions();
            then(userRepository).shouldHaveNoMoreInteractions();
            then(commentRepository).shouldHaveNoInteractions();
            then(commentMapper).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("삭제된 기사에는 등록할 수 없다.")
        void registerComment_DeletedArticle_ThrowsException() {
            // given
            markDeleted(article);
            given(newsArticleRepository.findById(articleId)).willReturn(Optional.of(article));

            // when & then
            assertThatThrownBy(() -> commentService.registerComment(request))
                    .isInstanceOf(BusinessException.class);

            then(newsArticleRepository).should().findById(articleId);
            then(newsArticleRepository).shouldHaveNoMoreInteractions();
            then(userRepository).shouldHaveNoInteractions();
            then(commentRepository).shouldHaveNoInteractions();
            then(commentMapper).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("삭제된 사용자로는 등록할 수 없다.")
        void registerComment_DeletedUser_ThrowsException() {
            // given
            markDeleted(user);
            given(newsArticleRepository.findById(articleId)).willReturn(Optional.of(article));
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // when & then
            assertThatThrownBy(() -> commentService.registerComment(request))
                    .isInstanceOf(BusinessException.class);

            then(newsArticleRepository).should().findById(articleId);
            then(userRepository).should().findById(userId);
            then(newsArticleRepository).shouldHaveNoMoreInteractions();
            then(userRepository).shouldHaveNoMoreInteractions();
            then(commentRepository).shouldHaveNoInteractions();
            then(commentMapper).shouldHaveNoInteractions();
        }
    }

    private void markDeleted(Object entity) {
        ReflectionTestUtils.setField(entity, "deleteStatus", DeleteStatus.DELETED);
        ReflectionTestUtils.setField(entity, "deletedAt", Instant.parse("2026-04-17T00:00:00Z"));
    }
}
