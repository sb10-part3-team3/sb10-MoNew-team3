package com.team3.monew.dto.notification;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.team3.monew.entity.enums.NotificationResourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record NotificationDto(
    @Schema(description = "알림 ID")
    UUID id,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", timezone = "UTC")
    @Schema(description = "생성된 날짜")
    Instant createdAt,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", timezone = "UTC")
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
