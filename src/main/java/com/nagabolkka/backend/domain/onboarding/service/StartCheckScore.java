package com.nagabolkka.backend.domain.onboarding.service;

import java.util.List;

/* 0~4 → 1 집 안 · 5~9 → 2 문 앞 · 10~14 → 3 동네 · 15~19 → 4 사람 곁 · 20~24 → 5 사회 연결 */
public final class StartCheckScore {

    public static final int QUESTION_COUNT = 8;
    public static final int MAX_ANSWER = 3;
    public static final int MAX_SCORE = QUESTION_COUNT * MAX_ANSWER; // 24

    private StartCheckScore() {
    }

    public static int sum(List<Integer> answers) {
        return answers.stream().mapToInt(Integer::intValue).sum();
    }

    public static int toStageNo(int score) {
        if (score < 0 || score > MAX_SCORE) {
            throw new IllegalArgumentException("점수는 0 ~ " + MAX_SCORE + " 이어야 해요: " + score);
        }
        if (score <= 4) return 1;
        if (score <= 9) return 2;
        if (score <= 14) return 3;
        if (score <= 19) return 4;
        return 5;
    }
}