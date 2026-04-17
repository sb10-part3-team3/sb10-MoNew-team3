package com.team3.monew.service;

import com.team3.monew.dto.data.CommentDto;
import com.team3.monew.dto.request.CommentRegisterRequest;
import com.team3.monew.entity.Comment;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.User;
import com.team3.monew.entity.enums.DeleteStatus;
import com.team3.monew.global.enums.ErrorCode;
import com.team3.monew.global.exception.BusinessException;
import com.team3.monew.mapper.CommentMapper;
import com.team3.monew.repository.CommentRepository;
import com.team3.monew.repository.NewsArticleRepository;
import com.team3.monew.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final NewsArticleRepository newsArticleRepository;

    @Transactional
    public CommentDto registerComment(CommentRegisterRequest request) {
        NewsArticle article = newsArticleRepository.findById(request.articleId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));
        validateActive(article);

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));
        validateActive(user);

        Comment comment = Comment.create(article, user, request.content());
        Comment savedComment = commentRepository.save(comment);

        return commentMapper.toDto(savedComment, false);
    }

    private void validateActive(NewsArticle article) {
        if (article.getDeleteStatus() == DeleteStatus.DELETED) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private void validateActive(User user) {
        if (user.getDeleteStatus() == DeleteStatus.DELETED) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
