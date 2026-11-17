package com.nagabolkka.backend.domain.letter.service;

// 편지 한 통의 통계 — outing · split 은 그 주, piece · foundSlots 는 만들 때 누적
public record LetterStats(
        int outingCount,
        int splitCount,
        int pieceCount,
        int foundSlots,
        String regionName,
        int restoration,
        Double moodAverage // 그 주 기분 평균 (기록 없으면 null) · AI 말투에만 쓰고 저장 안 함
) {
    public static final int TOTAL_SLOTS = 25;

    public String collection() {
        return foundSlots + "/" + TOTAL_SLOTS;
    }
}