package com.nagabolkka.backend.domain.record.controller;

import com.nagabolkka.backend.domain.record.dto.RecordsResponse;
import com.nagabolkka.backend.domain.record.service.RecordService;
import com.nagabolkka.backend.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@Tag(name = "모험 일지", description = "이번 주 외출 · 월간 캘린더 · 기분 흐름 · 지역 복원도")
@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    @Operation(summary = "모험 일지",
            description = "기록 화면 전체입니다.\n\n"
                    + "- `thisWeek` : `month` 와 상관없이 이번 주 월 ~ 일 7개 · `outingCount` = 이번 주 완료 수 (실내 미션 포함)\n"
                    + "- `month` : 고른 달 1일 ~ 말일 전부 (없으면 이번 달)\n"
                    + "- `status` : `COMPLETED` · `SKIPPED` · `null` (기록 없음 · 아직 진행 중) · `mood` 는 완료한 날만\n"
                    + "- `restoration` : 지역 5개 복원도 · 색\n\n"
                    + "회고 문장은 내려주지 않습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = RecordsResponse.class))),
            @ApiResponse(responseCode = "400", description = "month 형식 오류 (yyyy-MM 아님 · 13월 등)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "INVALID_INPUT", value = "{\"code\":\"INVALID_INPUT\",\"message\":\"입력값을 다시 확인해 주세요\"}"))),
            @ApiResponse(responseCode = "401", description = "토큰 없음 · 만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "UNAUTHORIZED", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"다시 로그인해 주세요\"}"),
                            @ExampleObject(name = "AUTH_TOKEN_EXPIRED", value = "{\"code\":\"AUTH_TOKEN_EXPIRED\",\"message\":\"로그인이 만료됐어요\"}")
                    })),
            @ApiResponse(responseCode = "409", description = "시작 체크 전",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "ONBOARDING_REQUIRED", value = "{\"code\":\"ONBOARDING_REQUIRED\",\"message\":\"시작 체크를 먼저 해 주세요\"}")))
    })
    @GetMapping
    public RecordsResponse getRecords(@Parameter(hidden = true) @AuthenticationPrincipal Long userId,
                                      @Parameter(description = "볼 달 yyyy-MM (선택, 없으면 이번 달)", example = "2026-11")
                                      @RequestParam(required = false) YearMonth month) {
        return recordService.getRecords(userId, month);
    }
}