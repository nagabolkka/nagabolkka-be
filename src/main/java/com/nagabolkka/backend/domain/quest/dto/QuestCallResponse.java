package com.nagabolkka.backend.domain.quest.dto;

import com.nagabolkka.backend.global.ai.AiClient.LiveSessionResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record QuestCallResponse(
        @Schema(description = "OpenAI Realtime 임시 키 · 브라우저가 이 키로 WebRTC 연결 (60초 안에 시작)", example = "ek_68af…")
        String clientSecret,
        @Schema(description = "임시 키 만료 시각 (unix 초)", example = "1795700000")
        long expiresAt,
        @Schema(description = "통화 모델", example = "gpt-realtime-2.1-mini")
        String model,
        @Schema(description = "한 번 통화할 수 있는 최대 시간 (초) · 앱이 이 시간이 되면 끊음", example = "600")
        int maxSeconds
) {
    public static QuestCallResponse of(LiveSessionResult result, int maxSeconds) {
        return new QuestCallResponse(result.clientSecret(), result.expiresAt(), result.model(), maxSeconds);
    }
}