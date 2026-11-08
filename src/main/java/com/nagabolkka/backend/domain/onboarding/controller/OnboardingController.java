package com.nagabolkka.backend.domain.onboarding.controller;

import com.nagabolkka.backend.domain.location.entity.LocationPurpose;
import com.nagabolkka.backend.domain.location.service.LocationLogService;
import com.nagabolkka.backend.domain.onboarding.dto.StartCheckRequest;
import com.nagabolkka.backend.domain.onboarding.dto.StartCheckResponse;
import com.nagabolkka.backend.domain.onboarding.service.OnboardingService;
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

@Tag(name = "온보딩", description = "시작 체크")
@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final LocationLogService locationLogService;

    @Operation(summary = "시작 체크",
            description = "8문항 답 · 구 · 고른 목표로 출발 지역을 정합니다. 한 번만 할 수 있습니다. (재검사 없음)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "시작 체크 완료",
                    content = @Content(schema = @Schema(implementation = StartCheckResponse.class))),
            @ApiResponse(responseCode = "400", description = "답 개수 · 범위 오류, 없는 구 · 목표 id",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "INVALID_INPUT", value = "{\"code\":\"INVALID_INPUT\",\"message\":\"입력값을 다시 확인해 주세요\"}"))),
            @ApiResponse(responseCode = "401", description = "토큰 없음 · 만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "UNAUTHORIZED", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"다시 로그인해 주세요\"}"),
                            @ExampleObject(name = "AUTH_TOKEN_EXPIRED", value = "{\"code\":\"AUTH_TOKEN_EXPIRED\",\"message\":\"로그인이 만료됐어요\"}")
                    })),
            @ApiResponse(responseCode = "409", description = "이미 시작 체크를 마친 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "ONBOARDING_ALREADY_DONE", value = "{\"code\":\"ONBOARDING_ALREADY_DONE\",\"message\":\"이미 시작 체크를 마쳤어요\"}")))
    })
    @PostMapping("/start-check")
    public StartCheckResponse startCheck(@Parameter(hidden = true) @AuthenticationPrincipal Long userId,
                                         @Valid @RequestBody StartCheckRequest request) {
        StartCheckResponse response = onboardingService.startCheck(userId, request);

        // 시작 체크가 커밋된 뒤 기록 (실패해도 200)
        locationLogService.record(userId, LocationPurpose.DISTRICT_CHECK);
        return response;
    }
}