package com.nagabolkka.backend.domain.practice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

// 위기 감지면 {"crisisDetected": true} 만
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TurnResponse(
        @Schema(description = "true → 연출 없이 도움 받기 (?from=practice)", example = "false") boolean crisisDetected,
        @Schema(description = "상대역 답변", example = "아 토요 북토크요! 이번 주 토요일 오후 2시에 있어요.") String reply,
        @Schema(description = "이번 턴 번호 (서버가 셈)", example = "1") Integer turn,
        @Schema(description = "다음 선택지 (마지막 턴이면 [])") List<String> choices,
        @Schema(description = "마지막 턴 → 17번 끝내기", example = "false") Boolean finished
) {

    public static TurnResponse crisis() {
        return new TurnResponse(true, null, null, null, null);
    }

    public static TurnResponse reply(String reply, int turn, List<String> choices, boolean finished) {
        return new TurnResponse(false, reply, turn, choices, finished);
    }
}