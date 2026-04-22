package com.team3.monew.dto.notification;

import com.team3.monew.entity.enums.NotificationResourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "커서 기반 페이지 응답")
public record CursorPageResponseNotificationDto(

    @Schema(description = "페이지 내용")
    List<NotificationDto> content,

    @Schema(description = "다음 페이지 커서(마지막 요소의 시간)")
    Instant nextCursor,

    @Schema(description = "다음 보조 커서(마지막 요소 아이디)")
    UUID nextAfter,

    @Schema(description = "페이지 크기")
    Integer size,

    @Schema(description = "총 요소 수")
    Long totalElements,

    @Schema(description = "다음 페이지 여부")
    Boolean hasNext
) {

  public record NotificationDto(

      @Schema(description = "알림 ID")
      UUID id,

      @Schema(description = "생성된 날짜")
      Instant createdAt,

      @Schema(description = "확인한 날짜")
      Instant updatedAt,

      @Schema(description = "확인 여부")
      Boolean confirmed,

      @Schema(description = "알림 대상 사용자 ID")
      UUID userId,

      @Schema(description = "내용")
      String content,

      @Schema(description = "관련된 리소스 유형")
      NotificationResourceType resourceType,

      @Schema(description = "관련된 리소스 ID")
      UUID resourceId
  ) {

  }
}
