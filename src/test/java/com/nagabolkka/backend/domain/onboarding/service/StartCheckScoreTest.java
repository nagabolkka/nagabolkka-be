package com.nagabolkka.backend.domain.onboarding.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StartCheckScoreTest {

    @Test
    @DisplayName("점수 = 8문항 답의 합")
    void sum() {
        assertThat(StartCheckScore.sum(List.of(2, 1, 0, 1, 0, 1, 2, 3))).isEqualTo(10);
        assertThat(StartCheckScore.sum(List.of(0, 0, 0, 0, 0, 0, 0, 0))).isZero();
        assertThat(StartCheckScore.sum(List.of(3, 3, 3, 3, 3, 3, 3, 3))).isEqualTo(24);
    }

    @ParameterizedTest(name = "{0}점 → {1}계단")
    @CsvSource({
            "0, 1", "4, 1",   // 집 안
            "5, 2", "9, 2",   // 문 앞
            "10, 3", "14, 3", // 동네
            "15, 4", "19, 4", // 사람 곁
            "20, 5", "24, 5"  // 사회 연결
    })
    @DisplayName("점수 구간 경계값")
    void toStageNo(int score, int expectedStageNo) {
        assertThat(StartCheckScore.toStageNo(score)).isEqualTo(expectedStageNo);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 25})
    @DisplayName("0 ~ 24 밖이면 예외 (요청 검증을 통과했다면 생길 수 없는 값)")
    void toStageNo_outOfRange(int score) {
        assertThatThrownBy(() -> StartCheckScore.toStageNo(score))
                .isInstanceOf(IllegalArgumentException.class);
    }
}