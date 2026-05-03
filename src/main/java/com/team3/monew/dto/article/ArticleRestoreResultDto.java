package com.team3.monew.dto.article;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ArticleRestoreResultDto(
    @Schema(description = "날짜")
    Instant restoreDate,

    @Schema(description = "복구된 기사 ID 목록")
    List<UUID> restoredArticleIds,

    @Schema(description = "복구된 기사 수")
    long restoredArticleCount
) {

}
