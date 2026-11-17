package com.nagabolkka.backend.domain.notice.dto;

import com.nagabolkka.backend.domain.notice.entity.Notice;
import com.nagabolkka.backend.domain.notice.entity.NoticeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

// 바꿀 것만 보냄 : {"status":"STOPPED"} 또는 {"title":"...","content":"..."}
public record NoticeUpdateRequest(
        @Schema(description = "ACTIVE 다시 게시 · STOPPED 게시 중지", example = "STOPPED", nullable = true)
        NoticeStatus status,

        @Schema(description = "제목 (최대 60자)", nullable = true)
        @Size(max = Notice.TITLE_MAX)
        String title,

        @Schema(description = "내용 (최대 500자)", nullable = true)
        @Size(max = Notice.CONTENT_MAX)
        String content
) {
    public boolean isEmpty() {
        return status == null && title == null && content == null;
    }
}