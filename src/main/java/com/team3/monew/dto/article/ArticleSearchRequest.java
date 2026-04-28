package com.team3.monew.dto.article;

import com.team3.monew.dto.article.internal.enums.ArticleDirection;
import com.team3.monew.dto.article.internal.enums.ArticleOrderBy;
import com.team3.monew.entity.enums.NewsSourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ArticleSearchRequest(
    @Schema(description = "검색어(제목, 요약)")
    String keyword,

    @Schema(description = "관심사 ID")
    UUID interestId,

    @Schema(description = "출처(포함)")
    List<NewsSourceType> sourceIn,

    @Schema(description = "날짜 시작(범위)")
    LocalDateTime publishDateFrom,

    @Schema(description = "날짜 끝(범위)")
    LocalDateTime publishDateTo,

    @Schema(requiredMode = RequiredMode.REQUIRED, description = "정렬 속성 이름")
    @NotNull
    ArticleOrderBy orderBy,

    @Schema(requiredMode = RequiredMode.REQUIRED, description = "정렬 방향 (ASC, DESC)")
    @NotNull
    ArticleDirection direction,

    @Schema(description = "커서 값")
    String cursor,

    @Schema(description = "보조 커서(createdAt) 값")
    Instant after,

    @Schema(requiredMode = RequiredMode.REQUIRED, description = "커서 페이지 크기")
    @NotNull @Min(1) @Max(100)
    Integer limit
) {

}
