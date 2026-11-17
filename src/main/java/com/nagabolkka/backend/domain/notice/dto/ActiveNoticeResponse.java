package com.nagabolkka.backend.domain.notice.dto;

import com.nagabolkka.backend.domain.notice.entity.Notice;
import io.swagger.v3.oas.annotations.media.Schema;

public record ActiveNoticeResponse(
        @Schema(description = "게시 중 공지 (없으면 null)", nullable = true) ActiveNotice notice
) {

    public record ActiveNotice(
            @Schema(example = "3") Long noticeId,
            @Schema(example = "서버 점검 안내") String title,
            @Schema(example = "10월 28일 목요일 14:00-16:00 서버를 점검할 예정입니다.") String content
    ) {
        public static ActiveNotice from(Notice notice) {
            return new ActiveNotice(notice.getId(), notice.getTitle(), notice.getContent());
        }
    }
}