package com.nagabolkka.backend.domain.record.dto;

import com.nagabolkka.backend.domain.quest.entity.QuestStatus;
import com.nagabolkka.backend.domain.quest.repository.DayRecord;
import com.nagabolkka.backend.domain.record.dto.RecordsResponse.DayItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecordsResponseTest {

    private static final LocalDate MON = LocalDate.of(2026, 11, 16);
    private static final LocalDate SUN = LocalDate.of(2026, 11, 22);

    @Test
    @DisplayName("기간 안 모든 날짜를 빠짐없이 · 기록 없는 날은 status · mood null")
    void days_fillsAllDates() {
        List<DayItem> days = RecordsResponse.days(MON, SUN,
                List.of(new DayRecord(LocalDate.of(2026, 11, 18), QuestStatus.COMPLETED, 5)));

        assertThat(days).hasSize(7);
        assertThat(days.get(0).date()).isEqualTo(MON);
        assertThat(days.get(6).date()).isEqualTo(SUN);
        assertThat(days.get(0).status()).isNull();
        assertThat(days.get(2).status()).isEqualTo(QuestStatus.COMPLETED);
        assertThat(days.get(2).mood()).isEqualTo(5);
    }

    @Test
    @DisplayName("배정 · 진행 중 → null (아직 한 날 아님) · 쉬기 → SKIPPED 기분 없음")
    void dayItem_status() {
        assertThat(DayItem.of(MON, new DayRecord(MON, QuestStatus.ASSIGNED, null)).status()).isNull();
        assertThat(DayItem.of(MON, new DayRecord(MON, QuestStatus.IN_PROGRESS, null)).status()).isNull();

        DayItem skipped = DayItem.of(MON, new DayRecord(MON, QuestStatus.SKIPPED, 3)); // 혹시 기분이 있어도
        assertThat(skipped.status()).isEqualTo(QuestStatus.SKIPPED);
        assertThat(skipped.mood()).isNull();
    }

    @Test
    @DisplayName("이번 주 외출 수 = 완료한 날 수 (쉬기 · 진행 중 제외)")
    void outingCount() {
        List<DayItem> week = RecordsResponse.days(MON, SUN, List.of(
                new DayRecord(MON, QuestStatus.COMPLETED, 4),
                new DayRecord(MON.plusDays(1), QuestStatus.COMPLETED, 2),
                new DayRecord(MON.plusDays(2), QuestStatus.SKIPPED, null),
                new DayRecord(MON.plusDays(3), QuestStatus.IN_PROGRESS, null)));

        RecordsResponse res = RecordsResponse.of(week, week, List.of());

        assertThat(res.thisWeek().outingCount()).isEqualTo(2);
    }
}