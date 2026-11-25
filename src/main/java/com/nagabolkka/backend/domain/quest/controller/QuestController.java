package com.nagabolkka.backend.domain.quest.controller;

import com.nagabolkka.backend.domain.location.entity.LocationPurpose;
import com.nagabolkka.backend.domain.location.service.LocationLogService;
import com.nagabolkka.backend.domain.quest.dto.CompleteQuestRequest;
import com.nagabolkka.backend.domain.quest.dto.CompleteQuestResponse;
import com.nagabolkka.backend.domain.quest.dto.QuestActionRequest;
import com.nagabolkka.backend.domain.quest.dto.TodayQuestResponse;
import com.nagabolkka.backend.domain.quest.service.QuestCompleteService;
import com.nagabolkka.backend.domain.quest.service.QuestService;
import com.nagabolkka.backend.global.exception.ErrorResponse;
import com.nagabolkka.backend.global.geo.GeoPoint;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "퀘스트", description = "오늘 퀘스트 배정 · 동작 · 완료")
@RestController
@RequestMapping("/api/quests")
@RequiredArgsConstructor
public class QuestController {

    private final QuestService questService;
    private final QuestCompleteService questCompleteService;
    private final LocationLogService locationLogService;

    @Operation(summary = "오늘 퀘스트",
            description = "오늘 퀘스트를 돌려줍니다. 아직 없으면 이때 배정합니다. (하루 1개, 여러 번 불러도 같은 퀘스트)\n\n"
                    + "- 배정 : 비 오는 날 현재 계단 비 미션 → 현재 계단 · 오늘 난이도 미션 (최근에 안 한 미션 먼저)\n"
                    + "- 지나온 계단 미션은 자동으로 배정하지 않아요. (동작 `REVISIT` 으로 직접 받기)\n"
                    + "- `lat` · `lng` (선택) : 바깥 미션의 근처 실제 장소 · 거리 기준. 없으면 내 구 중심 (좌표는 저장하지 않음)")
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
    public TodayQuestResponse getToday(@Parameter(hidden = true) @AuthenticationPrincipal Long userId,
                                       @Parameter(description = "현재 위도 (선택)", example = "37.5894")
                                       @RequestParam(required = false) Double lat,
                                       @Parameter(description = "현재 경도 (선택)", example = "127.0167")
                                       @RequestParam(required = false) Double lng) {
        return questService.getToday(userId, located(userId, lat, lng));
    }

    @Operation(summary = "오늘 퀘스트 동작",
            description = "오늘 퀘스트 버튼 동작을 처리하고, 바뀐 오늘 퀘스트를 돌려줍니다. (응답은 오늘 퀘스트와 같은 형태)\n\n"
                    + "- `ACCEPT` 수락 → `IN_PROGRESS` (이미 진행 중이면 그대로)\n"
                    + "- `SPLIT` 쪼개기 (수락 포함) → `isSplit: true` · `steps` 3개 (이미 쪼갰으면 그대로)\n"
                    + "- `CHANGE` 바꾸기 → 같은 계단 · 같은 난이도 다른 미션 · `ASSIGNED` 로 · 하루 2번 (`changeRemaining`)\n"
                    + "- `SKIP` 오늘은 쉬기 → `SKIPPED`\n"
                    + "- `REVISIT` 지나온 계단 미션 받기 (`stageNo` 필수) → 그 계단에서 못 찾은 조각 중 가장 쉬운 미션 · 바꾸기 횟수 1번 사용\n"
                    + "- `PRACTICE_MISSION` 연습 후 실제 미션 담기 (`missionId` 필수 = 연습 끝 응답의 `followMission.missionId`) "
                    + "→ `source: PRACTICE` · 계단은 내 현재 계단 · 바꾸기 횟수 그대로. "
                    + "오늘 끝낸 연습이 제안한 미션만 가능 (아니면 404), 이미 같은 미션이면 그대로\n"
                    + "- `EASIER` · `HARDER` 난이도 한 단계 쉽게 · 어렵게 → 같은 계단 · 고른 난이도 미션 (최근에 안 한 것 먼저). "
                    + "하루 1번 (`difficultyChangeRemaining`) · 수락 전 자동 배정 퀘스트만 · 바꾸기 횟수 그대로 · 내일 난이도는 그대로\n\n"
                    + "`lat` · `lng` (선택) : `CHANGE` · `REVISIT` · `PRACTICE_MISSION` · `EASIER` · `HARDER` 로 바깥 미션이 되면 근처 실제 장소 기준. 없으면 내 구 중심\n\n"
                    + "완료 · 쉬기 한 퀘스트에는 어떤 동작도 할 수 없어요. (409)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "동작 후 오늘 퀘스트",
                    content = @Content(schema = @Schema(implementation = TodayQuestResponse.class))),
            @ApiResponse(responseCode = "400", description = "type 없음 · 없는 type · REVISIT 인데 stageNo 없음 · 지나온 계단 아님 (현재 · 잠긴 계단) · "
                    + "PRACTICE_MISSION 인데 missionId 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "INVALID_INPUT", value = "{\"code\":\"INVALID_INPUT\",\"message\":\"입력값을 다시 확인해 주세요\"}"))),
            @ApiResponse(responseCode = "401", description = "토큰 없음 · 만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "UNAUTHORIZED", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"다시 로그인해 주세요\"}"),
                            @ExampleObject(name = "AUTH_TOKEN_EXPIRED", value = "{\"code\":\"AUTH_TOKEN_EXPIRED\",\"message\":\"로그인이 만료됐어요\"}")
                    })),
            @ApiResponse(responseCode = "404", description = "PRACTICE_MISSION : 오늘 끝낸 연습에서 제안받지 않은 미션 · 없는 미션",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "NOT_FOUND", value = "{\"code\":\"NOT_FOUND\",\"message\":\"찾을 수 없어요\"}"))),
            @ApiResponse(responseCode = "409", description = "이미 끝난 퀘스트 · 바꾸기 횟수 초과 (CHANGE · REVISIT) · "
                    + "난이도 바꾸기 1번 다 씀 · 지금은 난이도를 바꿀 수 없음 (수락 뒤 · 담기 · 연습 미션 · 끝 단계) · 시작 체크 전", // 🆕
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "QUEST_ALREADY_DONE", value = "{\"code\":\"QUEST_ALREADY_DONE\",\"message\":\"이미 끝난 퀘스트예요\"}"),
                            @ExampleObject(name = "QUEST_CHANGE_LIMIT", value = "{\"code\":\"QUEST_CHANGE_LIMIT\",\"message\":\"오늘은 더 바꿀 수 없어요\"}"),
                            @ExampleObject(name = "QUEST_DIFFICULTY_LIMIT", value = "{\"code\":\"QUEST_DIFFICULTY_LIMIT\",\"message\":\"오늘은 난이도를 이미 바꿨어요\"}"),
                            @ExampleObject(name = "QUEST_DIFFICULTY_LOCKED", value = "{\"code\":\"QUEST_DIFFICULTY_LOCKED\",\"message\":\"지금은 난이도를 바꿀 수 없어요\"}"),
                            @ExampleObject(name = "ONBOARDING_REQUIRED", value = "{\"code\":\"ONBOARDING_REQUIRED\",\"message\":\"시작 체크를 먼저 해 주세요\"}")
                    }))
    })
    @PostMapping("/today/actions")
    public TodayQuestResponse act(@Parameter(hidden = true) @AuthenticationPrincipal Long userId,
                                  @Valid @RequestBody QuestActionRequest request,
                                  @Parameter(description = "현재 위도 (선택, 바꾸기 · 지나온 계단 · 연습 후 미션 · 난이도 바꾸기 장소 기준)", example = "37.5894")
                                  @RequestParam(required = false) Double lat,
                                  @Parameter(description = "현재 경도 (선택)", example = "127.0167")
                                  @RequestParam(required = false) Double lng) {
        return questService.act(userId, request, located(userId, lat, lng));
    }

    @Operation(summary = "오늘 퀘스트 완료 · 기록",
            description = "오늘 퀘스트를 완료하고 기분 · 한 줄 회고를 기록합니다. 받은 조각 · 복원도 · 루미 한마디를 돌려줍니다.\n\n"
                    + "- 조각 : 미션 보상 조각 + 1계단 미션에서 처음 회고를 남기면 쉼의 조각 (`isNew` 로 NEW 뱃지)\n"
                    + "- 복원도 : 퀘스트 지역 +20% (지나온 계단 미션이면 그 계단, `restoration.stageNo`)\n"
                    + "- 쪼갠 퀘스트는 `doneSteps` 1 이상\n\n"
                    + "회고에서 위기 표현이 감지되면 `{\"crisisDetected\": true}` 만 돌려줍니다. (회고는 저장하지 않음, 조각 · 복원도는 처리)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "완료",
                    content = @Content(schema = @Schema(implementation = CompleteQuestResponse.class))),
            @ApiResponse(responseCode = "400", description = "입력값 오류 · 쪼갠 단계 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "INVALID_INPUT", value = "{\"code\":\"INVALID_INPUT\",\"message\":\"입력값을 다시 확인해 주세요\"}"),
                            @ExampleObject(name = "SPLIT_STEP_REQUIRED", value = "{\"code\":\"SPLIT_STEP_REQUIRED\",\"message\":\"한 단계 이상 해낸 뒤 완료할 수 있어요\"}")
                    })),
            @ApiResponse(responseCode = "401", description = "토큰 없음 · 만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "UNAUTHORIZED", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"다시 로그인해 주세요\"}"),
                            @ExampleObject(name = "AUTH_TOKEN_EXPIRED", value = "{\"code\":\"AUTH_TOKEN_EXPIRED\",\"message\":\"로그인이 만료됐어요\"}")
                    })),
            @ApiResponse(responseCode = "409", description = "이미 끝난 퀘스트 · 시작 체크 전",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "QUEST_ALREADY_DONE", value = "{\"code\":\"QUEST_ALREADY_DONE\",\"message\":\"이미 끝난 퀘스트예요\"}"),
                            @ExampleObject(name = "ONBOARDING_REQUIRED", value = "{\"code\":\"ONBOARDING_REQUIRED\",\"message\":\"시작 체크를 먼저 해 주세요\"}")
                    }))
    })
    @PostMapping("/today/complete")
    public CompleteQuestResponse complete(@Parameter(hidden = true) @AuthenticationPrincipal Long userId,
                                          @Valid @RequestBody CompleteQuestRequest request) {
        return questCompleteService.complete(userId, request);
    }

    // 위치를 받았으면 이용 사실만 기록 (좌표 저장 안 함)
    private GeoPoint located(Long userId, Double lat, Double lng) {
        GeoPoint location = GeoPoint.ofNullable(lat, lng);
        if (location != null) {
            locationLogService.record(userId, LocationPurpose.QUEST);
        }
        return location;
    }
}