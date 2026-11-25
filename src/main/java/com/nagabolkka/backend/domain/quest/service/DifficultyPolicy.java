package com.nagabolkka.backend.domain.quest.service;

import com.nagabolkka.backend.global.common.Difficulty;

// 다음 날 난이도 : 완료 기분 · 쉬기로 한 단계씩 (EASY ↔ NORMAL ↔ CHALLENGE)
// 사용자가 오늘 퀘스트 난이도를 고를 때도 한 단계씩
public final class DifficultyPolicy {

    private static final int MOOD_UP = 5;   // 좋았어요
    private static final int MOOD_DOWN = 2; // 그저 그래요 이하

    private DifficultyPolicy() {
    }

    // 기분 5 → 올림, 3 · 4 → 그대로, 1 · 2 → 내림 (프론트 NEXT_DIFFICULTY_MESSAGE 와 같은 기준)
    public static Difficulty afterComplete(Difficulty current, int mood) {
        if (mood >= MOOD_UP) {
            return up(current);
        }
        if (mood <= MOOD_DOWN) {
            return down(current);
        }
        return current;
    }

    // 오늘은 쉬기 → 내림
    public static Difficulty afterSkip(Difficulty current) {
        return down(current);
    }

    // 위기 감지 → 가장 쉬움
    public static Difficulty afterCrisis() {
        return Difficulty.EASY;
    }

    // 「더 쉽게」 : 한 단계 아래 (EASY 면 그대로 → 서비스에서 409)
    public static Difficulty easier(Difficulty current) {
        return down(current);
    }

    // 「더 어렵게」 : 한 단계 위 (CHALLENGE 면 그대로 → 서비스에서 409)
    public static Difficulty harder(Difficulty current) {
        return up(current);
    }

    private static Difficulty up(Difficulty current) {
        return switch (current) {
            case EASY -> Difficulty.NORMAL;
            case NORMAL, CHALLENGE -> Difficulty.CHALLENGE;
        };
    }

    private static Difficulty down(Difficulty current) {
        return switch (current) {
            case CHALLENGE -> Difficulty.NORMAL;
            case NORMAL, EASY -> Difficulty.EASY;
        };
    }
}