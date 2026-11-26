package com.nagabolkka.backend.domain.quest.controller;

import com.nagabolkka.backend.domain.quest.dto.QuestCallResponse;
import com.nagabolkka.backend.domain.quest.service.QuestCallService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "퀘스트", description = "오늘 퀘스트 배정 · 동작 · 완료")
@RestController
@RequestMapping("/api/quests")
@RequiredArgsConstructor
public class QuestCallController {

    private final QuestCallService questCallService;

    @Operation(summary = "루미 동행 통화 시작",
            description = "외출 중 루미와 이어폰으로 통화할 수 있도록 OpenAI Realtime 임시 키를 돌려줍니다.\n\n"
                    + "- 앱은 `clientSecret` 으로 `https://api.openai.com/v1/realtime/calls` 에 WebRTC 로 **직접** 연결 (음성은 우리 서버를 거치지 않음)\n"
                    + "- 루미에게 오늘 퀘스트 제목 · 목적지 · 닉네임만 전달 (기분 · 회고 등 마음 기록은 보내지 않음)\n"
                    + "- 키는 60초 안에 연결을 시작할 때만 유효 · 통화는 `maxSeconds` (10분) 가 되면 앱이 끊음\n"
                    + "- 녹음 · 자막은 서버에 저장하지 않음")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "임시 키",
                    content = @Content(schema = @Schema(implementation = QuestCallResponse.class))),
            @ApiResponse(responseCode = "404", description = "오늘 퀘스트가 아직 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "NOT_FOUND", value = "{\"code\":\"NOT_FOUND\",\"message\":\"찾을 수 없어요\"}"))),
            @ApiResponse(responseCode = "409", description = "시작 체크 전 · 이미 끝난 퀘스트 · AI 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "ONBOARDING_REQUIRED", value = "{\"code\":\"ONBOARDING_REQUIRED\",\"message\":\"시작 체크를 먼저 해 주세요\"}"),
                            @ExampleObject(name = "QUEST_ALREADY_DONE", value = "{\"code\":\"QUEST_ALREADY_DONE\",\"message\":\"이미 끝난 퀘스트예요\"}"),
                            @ExampleObject(name = "CALL_UNAVAILABLE", value = "{\"code\":\"CALL_UNAVAILABLE\",\"message\":\"지금은 루미와 통화할 수 없어요\"}")
                    }))
    })
    @PostMapping("/today/call")
    public QuestCallResponse startCall(@Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        return questCallService.start(userId);
    }
}