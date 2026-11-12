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
import com.nagabolkka.backend.domain.quest.dto.QuestActionRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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

    @Operation(summary = "오늘 퀘스트 동작",
            description = "오늘 퀘스트 상세 화면의 버튼 4개를 처리하고, 바뀐 오늘 퀘스트를 돌려줍니다. (응답은 오늘 퀘스트와 같은 형태)\n\n"
                    + "- `ACCEPT` 수락 → `IN_PROGRESS` (이미 진행 중이면 그대로)\n"
                    + "- `SPLIT` 쪼개기 (수락 포함) → `isSplit: true` · `steps` 3개 (이미 쪼갰으면 그대로)\n"
                    + "- `CHANGE` 바꾸기 → 같은 지역 다른 미션 · `ASSIGNED` 로 · 하루 2번 (`changeRemaining`)\n"
                    + "- `SKIP` 오늘은 쉬기 → `SKIPPED`\n\n"
                    + "완료 · 쉬기 한 퀘스트에는 어떤 동작도 할 수 없어요. (409)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "동작 후 오늘 퀘스트",
                    content = @Content(schema = @Schema(implementation = TodayQuestResponse.class))),
            @ApiResponse(responseCode = "400", description = "type 없음 · 없는 type",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "INVALID_INPUT", value = "{\"code\":\"INVALID_INPUT\",\"message\":\"입력값을 다시 확인해 주세요\"}"))),
            @ApiResponse(responseCode = "401", description = "토큰 없음 · 만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "UNAUTHORIZED", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"다시 로그인해 주세요\"}"),
                            @ExampleObject(name = "AUTH_TOKEN_EXPIRED", value = "{\"code\":\"AUTH_TOKEN_EXPIRED\",\"message\":\"로그인이 만료됐어요\"}")
                    })),
            @ApiResponse(responseCode = "409", description = "이미 끝난 퀘스트 · 바꾸기 횟수 초과 · 시작 체크 전",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "QUEST_ALREADY_DONE", value = "{\"code\":\"QUEST_ALREADY_DONE\",\"message\":\"이미 끝난 퀘스트예요\"}"),
                            @ExampleObject(name = "QUEST_CHANGE_LIMIT", value = "{\"code\":\"QUEST_CHANGE_LIMIT\",\"message\":\"오늘은 더 바꿀 수 없어요\"}"),
                            @ExampleObject(name = "ONBOARDING_REQUIRED", value = "{\"code\":\"ONBOARDING_REQUIRED\",\"message\":\"시작 체크를 먼저 해 주세요\"}")
                    }))
    })
    @PostMapping("/today/actions")
    public TodayQuestResponse act(@Parameter(hidden = true) @AuthenticationPrincipal Long userId,
                                  @Valid @RequestBody QuestActionRequest request) {
        return questService.act(userId, request);
    }
}