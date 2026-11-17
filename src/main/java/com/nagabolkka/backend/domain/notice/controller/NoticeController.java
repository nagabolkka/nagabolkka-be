package com.nagabolkka.backend.domain.notice.controller;

import com.nagabolkka.backend.domain.notice.dto.ActiveNoticeResponse;
import com.nagabolkka.backend.domain.notice.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "공지", description = "게시 중 공지 (앱 실행 시)")
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "게시 중 공지",
            description = "게시 중 (ACTIVE) 공지 중 가장 최근 1개입니다. 없으면 notice: null 이며 로그인 없이 호출할 수 있습니다. "
                    + "\"오늘 하루 보지 않기\" 는 프론트에서 처리합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = ActiveNoticeResponse.class)))
    @GetMapping("/active")
    public ActiveNoticeResponse getActive() {
        return noticeService.getActive();
    }
}