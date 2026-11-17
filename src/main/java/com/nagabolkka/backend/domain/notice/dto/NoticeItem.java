package com.nagabolkka.backend.domain.notice.dto;

import com.nagabolkka.backend.domain.notice.entity.Notice;
import com.nagabolkka.backend.domain.notice.entity.NoticeStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record NoticeItem(
        @Schema(example = "3") Long noticeId,
        @Schema(example = "서버 점검 안내") String title,
        @Schema(example = "10월 28일 목요일 14:00-16:00 서버를 점검할 예정입니다.") String content,
        @Schema(description = "ACTIVE 게시 중 · STOPPED 중지", example = "ACTIVE") NoticeStatus status,
        @Schema(example = "2026-10-20T10:00:00") LocalDateTime createdAt
) {
    public static NoticeItem from(Notice notice) {
        return new NoticeItem(notice.getId(), notice.getTitle(), notice.getContent(),
                notice.getStatus(), notice.getCreatedAt());
    }
}