package com.nagabolkka.backend.domain.quest.service;

import com.nagabolkka.backend.global.common.Difficulty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class DifficultyPolicyTest {

    @ParameterizedTest(name = "{0} + 기분 {1} → {2}")
    @DisplayName("완료 : 기분 5 올림 · 3 · 4 그대로 · 1 · 2 내림 (끝에서는 그대로)")
    @CsvSource({
            "EASY, 5, NORMAL",
            "NORMAL, 5, CHALLENGE",
            "CHALLENGE, 5, CHALLENGE",
            "NORMAL, 4, NORMAL",
            "NORMAL, 3, NORMAL",
            "NORMAL, 2, EASY",
            "CHALLENGE, 1, NORMAL",
            "EASY, 1, EASY"
    })
    void afterComplete(Difficulty current, int mood, Difficulty expected) {
        assertThat(DifficultyPolicy.afterComplete(current, mood)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} 쉬기 → {1}")
    @DisplayName("오늘은 쉬기 : 한 단계 내림 (EASY 면 그대로)")
    @CsvSource({"CHALLENGE, NORMAL", "NORMAL, EASY", "EASY, EASY"})
    void afterSkip(Difficulty current, Difficulty expected) {
        assertThat(DifficultyPolicy.afterSkip(current)).isEqualTo(expected);
    }

    @Test
    @DisplayName("위기 감지 : 가장 쉬움")
    void afterCrisis() {
        assertThat(DifficultyPolicy.afterCrisis()).isEqualTo(Difficulty.EASY);
    }
}