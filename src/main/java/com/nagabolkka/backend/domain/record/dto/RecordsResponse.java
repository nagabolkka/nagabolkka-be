package com.nagabolkka.backend.domain.record.dto;

import com.nagabolkka.backend.domain.quest.entity.QuestStatus;
import com.nagabolkka.backend.domain.quest.repository.DayRecord;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.domain.region.entity.UserRegion;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public record RecordsResponse(
        @Schema(description = "이번 주 (월 ~ 일, month 와 상관없음)") ThisWeek thisWeek,
        @Schema(description = "고른 달 1일 ~ 말일 (기록 없는 날도 포함)") List<DayItem> month,
        @Schema(description = "지역 복원도 (계단 순 5개)") List<RegionItem> restoration
) {

    public record ThisWeek(
            @Schema(description = "이번 주 완료 수 (실내 미션 포함)", example = "3") int outingCount,
            @Schema(description = "월 ~ 일 7개") List<DayItem> days
    ) {
    }

    public record DayItem(
            @Schema(example = "2026-11-16") LocalDate date,
            @Schema(description = "COMPLETED · SKIPPED · null (기록 없음 · 진행 중)", example = "COMPLETED", nullable = true)
            QuestStatus status,
            @Schema(description = "기분 1 ~ 5 (완료한 날만)", example = "4", nullable = true) Integer mood
    ) {

        // 완료 · 쉬기만 보여줌 (배정 · 진행 중은 아직 "한 날" 이 아님) · 기분은 완료일 때만
        public static DayItem of(LocalDate date, DayRecord record) {
            if (record == null) {
                return new DayItem(date, null, null);
            }
            return switch (record.status()) {
                case COMPLETED -> new DayItem(date, QuestStatus.COMPLETED, record.mood());
                case SKIPPED -> new DayItem(date, QuestStatus.SKIPPED, null);
                default -> new DayItem(date, null, null);
            };
        }
    }

    public record RegionItem(
            @Schema(example = "1") int stageNo,
            @Schema(example = "시작의 방") String name,
            @Schema(description = "지역 색 (복원도 막대)", example = "#E8C06A") String color,
            @Schema(example = "100") int restoration
    ) {
        public static RegionItem of(UserRegion userRegion) {
            Region region = userRegion.getRegion();
            return new RegionItem(region.getStageNo(), region.getName(), region.getColor(), userRegion.getRestoration());
        }
    }

    // from ~ to 모든 날짜를 빠짐없이 (기록 없는 날 = null)
    public static List<DayItem> days(LocalDate from, LocalDate to, List<DayRecord> records) {
        Map<LocalDate, DayRecord> byDate = records.stream()
                .collect(Collectors.toMap(DayRecord::date, Function.identity(), (a, b) -> a)); // 하루 1개지만 안전하게
        return from.datesUntil(to.plusDays(1))
                .map(date -> DayItem.of(date, byDate.get(date)))
                .toList();
    }

    public static RecordsResponse of(List<DayItem> week, List<DayItem> month, List<UserRegion> userRegions) {
        int outingCount = (int) week.stream().filter(day -> day.status() == QuestStatus.COMPLETED).count();
        return new RecordsResponse(
                new ThisWeek(outingCount, week),
                month,
                userRegions.stream().map(RegionItem::of).toList());
    }
}