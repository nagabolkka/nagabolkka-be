package com.nagabolkka.backend.global.weather;

import io.swagger.v3.oas.annotations.media.Schema;

// 지금 날씨 (기상청 초단기실황 PTY 기준) — 홈 표시 · 퀘스트 배정
public record Weather(
        @Schema(description = "CLEAR · RAIN · SNOW", example = "CLEAR") String sky,
        @Schema(example = "false") boolean isRainy
) {

    public static final String CLEAR = "CLEAR";
    public static final String RAIN = "RAIN";
    public static final String SNOW = "SNOW";

    public static Weather clear() {
        return new Weather(CLEAR, false);
    }

    // PTY : 0 없음 · 1 비 · 2 비/눈 · 3 눈 · 5 빗방울 · 6 빗방울눈날림 · 7 눈날림 (4 소나기는 단기예보에만)
    public static Weather fromPty(String pty) {
        if (pty == null) {
            return clear();
        }
        return switch (pty.trim()) {
            case "1", "2", "4", "5", "6" -> new Weather(RAIN, true);
            case "3", "7" -> new Weather(SNOW, true); // 눈도 가까운 곳 미션 · 비 배경
            default -> clear();
        };
    }
}