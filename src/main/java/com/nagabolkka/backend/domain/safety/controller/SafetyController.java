package com.nagabolkka.backend.domain.safety.controller;

import com.nagabolkka.backend.domain.safety.dto.SafetyEventRequest;
import com.nagabolkka.backend.domain.safety.service.SafetyService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "안전", description = "도움 받기 화면 열림 기록")
@RestController
@RequestMapping("/api/safety")
@RequiredArgsConstructor
public class SafetyController {

    private final SafetyService safetyService;

    @Operation(summary = "도움 받기 열림 기록",
            description = "마이페이지에서 도움 받기를 직접 열었을 때만 호출합니다. (source = MANUAL) \n\n"
                    + "회고 · 연습 중 위기 감지는 서버가 직접 기록하므로 REFLECTION · PRACTICE 는 400 입니다. \n\n"
                    + "1분 안에 다시 열면 저장하지 않고 204 를 돌려줍니다. 원문 · 위치는 남기지 않습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "기록 완료 (또는 1분 안 중복)"),
            @ApiResponse(responseCode = "400", description = "source 누락 · MANUAL 이 아닌 값",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "INVALID_INPUT", value = "{\"code\":\"INVALID_INPUT\",\"message\":\"입력값을 다시 확인해 주세요\"}"))),
            @ApiResponse(responseCode = "401", description = "토큰 없음 · 만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "UNAUTHORIZED", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"다시 로그인해 주세요\"}"),
                            @ExampleObject(name = "AUTH_TOKEN_EXPIRED", value = "{\"code\":\"AUTH_TOKEN_EXPIRED\",\"message\":\"로그인이 만료됐어요\"}")
                    }))
    })
    @PostMapping("/events")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void record(@Parameter(hidden = true) @AuthenticationPrincipal Long userId,
                       @Valid @RequestBody SafetyEventRequest request) {
        safetyService.record(userId, request.source());
    }
}