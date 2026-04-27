package com.team3.monew.dto.comment;

import com.team3.monew.entity.Comment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentUpdateRequest(
    @NotBlank @Size(min = 1, max = Comment.MAX_CONTENT_LENGTH)
    String content
) {
}
