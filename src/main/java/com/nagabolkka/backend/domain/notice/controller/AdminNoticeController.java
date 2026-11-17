package com.nagabolkka.backend.domain.notice.controller;

import com.nagabolkka.backend.domain.notice.dto.NoticeCreateRequest;
import com.nagabolkka.backend.domain.notice.dto.NoticeItem;
import com.nagabolkka.backend.domain.notice.dto.NoticeUpdateRequest;
import com.nagabolkka.backend.domain.notice.dto.NoticesResponse;
import com.nagabolkka.backend.domain.notice.service.NoticeService;
import com.nagabolkka.backend.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 · 공지", description = "공지 목록 · 등록 · 수정 · 게시 중지 · 삭제 (ADMIN)")
@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "토큰 없음 · 만료",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                @ExampleObject(name = "UNAUTHORIZED", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"다시 로그인해 주세요\"}"))),
        @ApiResponse(responseCode = "403", description = "관리자 아님",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                @ExampleObject(name = "FORBIDDEN", value = "{\"code\":\"FORBIDDEN\",\"message\":\"접근 권한이 없어요\"}")))
})
public class AdminNoticeController {

    private static final String INVALID = "{\"code\":\"INVALID_INPUT\",\"message\":\"입력값을 다시 확인해 주세요\"}";
    private static final String NOT_FOUND = "{\"code\":\"NOT_FOUND\",\"message\":\"찾을 수 없어요\"}";

    private final NoticeService noticeService;

    @Operation(summary = "공지 목록", description = "전체 공지 (최근 등록 순)")
    @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = NoticesResponse.class)))
    @GetMapping
    public NoticesResponse getAll() {
        return noticeService.getAll();
    }

    @Operation(summary = "공지 등록", description = "등록하면 바로 게시 중(ACTIVE)입니다. 제목 60자 · 내용 500자.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록",
                    content = @Content(schema = @Schema(implementation = NoticeItem.class))),
            @ApiResponse(responseCode = "400", description = "제목 · 내용 없음 · 길이 초과",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "INVALID_INPUT", value = INVALID)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NoticeItem create(@Valid @RequestBody NoticeCreateRequest request) {
        return noticeService.create(request);
    }

    @Operation(summary = "공지 수정 · 게시 중지 / 다시 게시",
            description = "보낸 필드만 바꿉니다. {\"status\":\"STOPPED\"} 또는 {\"title\":\"...\",\"content\":\"...\"}. "
                    + "아무것도 없거나 빈 제목 · 내용은 400.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정",
                    content = @Content(schema = @Schema(implementation = NoticeItem.class))),
            @ApiResponse(responseCode = "400", description = "바꿀 값 없음 · 빈 값 · 길이 초과 · 없는 status",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "INVALID_INPUT", value = INVALID))),
            @ApiResponse(responseCode = "404", description = "없는 공지",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "NOT_FOUND", value = NOT_FOUND)))
    })
    @PatchMapping("/{noticeId}")
    public NoticeItem update(@Parameter(description = "공지 id", example = "3") @PathVariable Long noticeId,
                             @Valid @RequestBody NoticeUpdateRequest request) {
        return noticeService.update(noticeId, request);
    }

    @Operation(summary = "공지 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제"),
            @ApiResponse(responseCode = "404", description = "없는 공지",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "NOT_FOUND", value = NOT_FOUND)))
    })
    @DeleteMapping("/{noticeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "공지 id", example = "3") @PathVariable Long noticeId) {
        noticeService.delete(noticeId);
    }
}