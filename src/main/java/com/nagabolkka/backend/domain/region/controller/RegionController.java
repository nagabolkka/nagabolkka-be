package com.nagabolkka.backend.domain.region.controller;

import com.nagabolkka.backend.domain.region.dto.NextRegionRequest;
import com.nagabolkka.backend.domain.region.dto.NextRegionResponse;
import com.nagabolkka.backend.domain.region.service.RegionService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "지역", description = "다음 지역 제안 응답")
@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    @Operation(summary = "다음 지역 응답",
            description = "현재 지역 복원도가 100% 일 때 홈에 뜨는 다음 지역 제안 (`nextRegionSuggestion`) 에 응답합니다.\n\n"
                    + "- `accept: true` 열어볼게요 → 다음 지역 ACTIVE · 지금 지역 COMPLETED · 현재 지역 변경\n"
                    + "- `accept: false` 지금이 좋아요 → 지금 지역에 머물고 7일 뒤 다시 제안\n\n"
                    + "홈에 제안이 없을 때 (복원도 100% 아님 · 5계단 · 머물기 후 7일 안 지남) 는 409 가 뜨며 오늘 퀘스트는 바뀌지 않아요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "응답 후 지금 머무는 지역",
                    content = @Content(schema = @Schema(implementation = NextRegionResponse.class))),
            @ApiResponse(responseCode = "400", description = "accept 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "INVALID_INPUT", value = "{\"code\":\"INVALID_INPUT\",\"message\":\"입력값을 다시 확인해 주세요\"}"))),
            @ApiResponse(responseCode = "401", description = "토큰 없음 · 만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "UNAUTHORIZED", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"다시 로그인해 주세요\"}"),
                            @ExampleObject(name = "AUTH_TOKEN_EXPIRED", value = "{\"code\":\"AUTH_TOKEN_EXPIRED\",\"message\":\"로그인이 만료됐어요\"}")
                    })),
            @ApiResponse(responseCode = "409", description = "제안 중이 아님 · 시작 체크 전",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "REGION_NOT_SUGGESTED", value = "{\"code\":\"REGION_NOT_SUGGESTED\",\"message\":\"지금은 다음 지역으로 갈 수 없어요\"}"),
                            @ExampleObject(name = "ONBOARDING_REQUIRED", value = "{\"code\":\"ONBOARDING_REQUIRED\",\"message\":\"시작 체크를 먼저 해 주세요\"}")
                    }))
    })
    @PostMapping("/next")
    public NextRegionResponse respondNext(@Parameter(hidden = true) @AuthenticationPrincipal Long userId,
                                          @Valid @RequestBody NextRegionRequest request) {
        return regionService.respondNext(userId, request);
    }
}