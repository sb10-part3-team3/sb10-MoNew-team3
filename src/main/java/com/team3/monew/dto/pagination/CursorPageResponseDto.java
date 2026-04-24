package com.team3.monew.dto.pagination;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "커서 기반 페이지 응답")
public record CursorPageResponseDto<T>(

    @Schema(description = "페이지 내용")
    List<T> content,

    @Schema(description = "다음 페이지 커서")
    String nextCursor,

    @Schema(description = "다음 보조 커서(마지막 요소의 생성 시간)")
    Instant nextAfter,

    @Schema(description = "페이지 크기")
    Integer size,

    @Schema(description = "총 요소 수")
    Long totalElements,

    @Schema(description = "다음 페이지 여부")
    Boolean hasNext
) {

}
