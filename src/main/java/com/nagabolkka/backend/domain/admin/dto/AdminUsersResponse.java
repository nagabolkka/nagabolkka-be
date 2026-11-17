package com.nagabolkka.backend.domain.admin.dto;

import com.nagabolkka.backend.domain.plus.entity.PriceOption;
import com.nagabolkka.backend.domain.plus.repository.PriceOptionCount;
import com.nagabolkka.backend.domain.quest.repository.QuestStats;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.global.common.MaskingUtil;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record AdminUsersResponse(Summary summary, List<UserItem> users) {

    public record Summary(
            @Schema(description = "전체 사용자 (ADMIN 제외)", example = "6") int totalUsers,
            @Schema(description = "평균 달성률 % (퀘스트가 1번 이상 배정된 사용자)", example = "55") int avgAchievementRate,
            PlusInterestSummary plusInterest
    ) {
    }

    public record PlusInterestSummary(
            @Schema(description = "설문 응답한 사용자 수", example = "4") long total,
            @Schema(description = "선택지별 응답 수 (3개 모두 · 0 포함)", example = "{\"PRICE_1900\":2,\"PRICE_3900\":1,\"CUSTOM\":1}")
            Map<PriceOption, Long> byOption,
            @Schema(description = "직접 입력 평균 (원 · 반올림 · 없으면 null)", example = "2500", nullable = true) Integer customPriceAvg
    ) {
        public static PlusInterestSummary of(List<PriceOptionCount> counts) {
            Map<PriceOption, Long> byOption = new LinkedHashMap<>();
            for (PriceOption option : PriceOption.values()) {
                byOption.put(option, 0L);
            }
            Integer customAvg = null;
            for (PriceOptionCount count : counts) {
                byOption.put(count.priceOption(), count.count());
                if (count.priceOption() == PriceOption.CUSTOM && count.customPriceAvg() != null) {
                    customAvg = (int) Math.round(count.customPriceAvg());
                }
            }
            long total = byOption.values().stream().mapToLong(Long::longValue).sum();
            return new PlusInterestSummary(total, byOption, customAvg);
        }
    }

    public record UserItem(
            @Schema(description = "마스킹된 표시 이름", example = "홍*지") String label,
            @Schema(description = "현재 계단 (시작 체크 전이면 null)", example = "1", nullable = true) Integer stageNo,
            @Schema(description = "퀘스트가 배정된 날", example = "11") long assigned,
            @Schema(example = "8") long completed,
            @Schema(example = "2") long skipped,
            @Schema(description = "완료 ÷ 배정 × 100 (반올림)", example = "73") int achievementRate,
            @Schema(description = "마지막 접속일 (홈을 연 날 · 없으면 null)", example = "2026-10-14", nullable = true) LocalDate lastActiveAt
    ) {
        public static UserItem of(User user, QuestStats stats) {
            long assigned = stats == null ? 0 : stats.assigned();
            long completed = stats == null ? 0 : stats.completed();
            long skipped = stats == null ? 0 : stats.skipped();
            return new UserItem(
                    MaskingUtil.name(user.getNickname()),
                    user.getCurrentRegion() == null ? null : user.getCurrentRegion().getStageNo(),
                    assigned, completed, skipped,
                    rate(completed, assigned),
                    user.getLastActiveAt() == null ? null : user.getLastActiveAt().toLocalDate());
        }

        static int rate(long completed, long assigned) {
            return assigned == 0 ? 0 : (int) Math.round(completed * 100.0 / assigned);
        }
    }

    public static AdminUsersResponse of(List<UserItem> users, PlusInterestSummary plusInterest) {
        int avg = (int) Math.round(users.stream()
                .filter(user -> user.assigned() > 0)
                .mapToInt(UserItem::achievementRate)
                .average()
                .orElse(0));
        return new AdminUsersResponse(new Summary(users.size(), avg, plusInterest), users);
    }
}