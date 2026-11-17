package com.nagabolkka.backend.domain.letter.batch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class WeeklyLetterJobTest {

    @Test
    @DisplayName("월요일 (배치 도는 날) → 지난주 월요일")
    void lastWeekStart_monday() {
        assertThat(WeeklyLetterJob.lastWeekStart(LocalDate.of(2026, 11, 16))).isEqualTo(LocalDate.of(2026, 11, 9));
    }

    @Test
    @DisplayName("수요일 (수동 실행) → 지난주 월요일")
    void lastWeekStart_wednesday() {
        assertThat(WeeklyLetterJob.lastWeekStart(LocalDate.of(2026, 11, 18))).isEqualTo(LocalDate.of(2026, 11, 9));
    }
}