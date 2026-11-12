package com.nagabolkka.backend.domain.quest.controller;

import com.nagabolkka.backend.domain.quest.dto.TodayQuestResponse;
import com.nagabolkka.backend.domain.quest.service.QuestService;
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

@Tag(name = "퀘스트", description = "오늘 퀘스트 배정 · 동작 · 완료")
@RestController
@RequestMapping("/api/quests")
@RequiredArgsConstructor
public class QuestController {

    private final QuestService questService;
    @Operation(summary = "오늘 퀘스트",
            description = "오늘 퀘스트를 돌려줍니다. 아직 없으면 이때 배정합니다. (하루 1개, 여러 번 불러도 같은 퀘스트)\n\n"
                    + "쉬운 날 (EASY) 에는 지나온 계단에서 아직 못 찾은 조각 미션이 먼저 배정됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "오늘 퀘스트",
                    content = @Content(schema = @Schema(implementation = TodayQuestResponse.class))),
            @ApiResponse(responseCode = "401", description = "토큰 없음 · 만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "UNAUTHORIZED", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"다시 로그인해 주세요\"}"),
                            @ExampleObject(name = "AUTH_TOKEN_EXPIRED", value = "{\"code\":\"AUTH_TOKEN_EXPIRED\",\"message\":\"로그인이 만료됐어요\"}")
                    })),
            @ApiResponse(responseCode = "409", description = "시작 체크 전",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "ONBOARDING_REQUIRED", value = "{\"code\":\"ONBOARDING_REQUIRED\",\"message\":\"시작 체크를 먼저 해 주세요\"}")))
    })
    @GetMapping("/today")
    public TodayQuestResponse getToday(@Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        return questService.getToday(userId);
    }
}