package com.nagabolkka.backend.domain.notice.dto;

import com.nagabolkka.backend.domain.notice.entity.Notice;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoticeCreateRequest(
        @Schema(description = "제목 (최대 60자)", example = "서버 점검 안내")
        @NotBlank @Size(max = Notice.TITLE_MAX)
        String title,

        @Schema(description = "내용 (최대 500자)", example = "10월 28일 목요일 14:00-16:00 서버를 점검할 예정입니다.")
        @NotBlank @Size(max = Notice.CONTENT_MAX)
        String content
) {
}