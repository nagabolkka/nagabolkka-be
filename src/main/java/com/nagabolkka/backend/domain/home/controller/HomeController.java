package com.nagabolkka.backend.domain.home.controller;

import com.nagabolkka.backend.domain.home.dto.HomeResponse;
import com.nagabolkka.backend.domain.home.service.HomeService;
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
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "홈", description = "홈 화면 전체")
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @Operation(summary = "홈",
            description = "홈 화면 전체를 한 번에 돌려줍니다. 오늘 퀘스트가 없으면 이때 배정합니다. (오늘 퀘스트 API 와 같은 퀘스트)\n\n"
                    + "- 모험의 길 `regions[].status` : LOCKED · ACTIVE · COMPLETED · PASSED (지나온 곳)\n"
                    + "- 마지막 접속이 3일 이상 전이면 그날 첫 홈에서 `homeState: WELCOME_BACK`\n")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "홈 화면",
                    content = @Content(schema = @Schema(implementation = HomeResponse.class))),
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
    public HomeResponse getHome(@Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        return homeService.getHome(userId);
    }
}