package com.team3.monew.dto.comment;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CommentRegisterRequest(
        @NotNull
        UUID articleId,
        @NotBlank @Size(min = 1, max = 500)
        String content
) {
}
