package com.nagabolkka.backend.domain.quest.service;

import com.nagabolkka.backend.domain.mission.entity.Mission;
import com.nagabolkka.backend.global.common.Difficulty;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class QuestAssigner {

    // 최근 며칠 미션까지 보고 겹치지 않게 (같은 계단 · 난이도 미션 5개 + 여유)
    public static final int RECENT_DAYS = 10;

    // 최근 기록 없이 (테스트용)
    public Mission pick(int currentStageNo, Difficulty difficulty, boolean rainy, List<Mission> candidates) {
        return pick(currentStageNo, difficulty, rainy, candidates, List.of());
    }

    // 자동 배정 : ① 비 오는 날 → ② 현재 계단 · 오늘 난이도 (최근에 안 한 미션 먼저)
    // recentMissionIds : 최근 퀘스트 미션 id (0번 = 가장 최근)
    public Mission pick(int currentStageNo, Difficulty difficulty, boolean rainy,
                        List<Mission> candidates, List<Long> recentMissionIds) {

        List<Mission> current = candidates.stream()
                .filter(m -> m.getRegion().getStageNo() == currentStageNo)
                .toList();

        // 1. 비 오는 날
        if (rainy) {
            Optional<Mission> rainyMission = current.stream()
                    .filter(Mission::isRainyOk)
                    .min(Comparator.comparing((Mission m) -> !m.isRainyOnly())); // 빗소리 칸 우선
            if (rainyMission.isPresent()) {
                return rainyMission.get();
            }
        }

        // 2. 현재 계단 · 오늘 난이도 (없으면 현재 계단 쉬운 미션)
        Comparator<Mission> leastRecent = leastRecentFirst(recentMissionIds);
        return current.stream()
                .filter(m -> !m.isRainyOnly())
                .filter(m -> m.getDifficulty() == difficulty)
                .min(leastRecent)
                .or(() -> current.stream()
                        .filter(m -> !m.isRainyOnly())
                        .filter(m -> m.getDifficulty() == Difficulty.EASY)
                        .min(leastRecent))
                .orElseThrow(() -> new IllegalStateException("미션 시드가 없어요: " + currentStageNo + "계단")); // 시드 누락 = 버그 → 500
    }

    // 지나온 계단 미션 받기 : 그 계단에서 못 찾은 조각 중 가장 쉬운 난이도 (다 찾았으면 쉬움) · 같은 난이도 안에서는 최근에 안 한 미션
    public Mission pickRevisit(List<Mission> stageMissions, Set<Long> foundPieceIds, List<Long> recentMissionIds) {
        List<Mission> options = stageMissions.stream()
                .filter(m -> !m.isRainyOnly())
                .toList();

        Difficulty target = options.stream()
                .filter(m -> !foundPieceIds.contains(m.getRewardPiece().getId()))
                .map(Mission::getDifficulty)
                .min(Comparator.naturalOrder()) // EASY → NORMAL → CHALLENGE
                .orElse(Difficulty.EASY);

        return options.stream()
                .filter(m -> m.getDifficulty() == target)
                .min(leastRecentFirst(recentMissionIds))
                .orElseThrow(() -> new IllegalStateException("지나온 계단 미션 시드가 없어요")); // 시드 누락 = 버그 → 500
    }

    // 난이도 바꾸기 : 퀘스트 계단 · 고른 난이도 · 빗소리 제외 · 최근에 안 한 미션 먼저
    public Mission pickDifficulty(int stageNo, Difficulty target, List<Mission> candidates,
                                  List<Long> recentMissionIds) {
        return candidates.stream()
                .filter(m -> m.getRegion().getStageNo() == stageNo)
                .filter(m -> m.getDifficulty() == target)
                .filter(m -> !m.isRainyOnly())
                .min(leastRecentFirst(recentMissionIds))
                .orElseThrow(() -> new IllegalStateException(
                        "난이도 바꿀 미션 시드가 없어요: " + stageNo + "계단 " + target)); // 시드 누락 = 버그 → 500
    }

    // 같은 계단 · 같은 난이도 안에서 다음 미션 (id 순으로 돌아감 · 빗소리 제외)
    public Mission pickChange(Mission current, List<Mission> candidates) {
        return pickChange(current, current.getRegion().getStageNo(), candidates);
    }

    // stageNo = 퀘스트 계단 (연습 후 미션은 미션 계단과 다를 수 있음)
    public Mission pickChange(Mission current, int stageNo, List<Mission> candidates) {
        List<Mission> options = candidates.stream()
                .filter(m -> m.getRegion().getStageNo() == stageNo)
                .filter(m -> m.getDifficulty() == current.getDifficulty())
                .filter(m -> !m.isRainyOnly())
                .filter(m -> !m.getId().equals(current.getId()))
                .sorted(Comparator.comparing(Mission::getId))
                .toList();

        return options.stream()
                .filter(m -> m.getId() > current.getId())
                .findFirst()
                .or(() -> options.stream().findFirst())
                .orElseThrow(() -> new IllegalStateException(
                        "바꿀 미션 시드가 없어요: " + stageNo + "계단 " + current.getDifficulty())); // 시드 누락 = 버그 → 500
    }

    // 한 적 없는 미션 → 오래전에 한 미션 → 최근에 한 미션 순 (같으면 id 순)
    private static Comparator<Mission> leastRecentFirst(List<Long> recentMissionIds) {
        return Comparator.comparingInt((Mission m) -> {
                    int index = recentMissionIds.indexOf(m.getId()); // 0 = 가장 최근, -1 = 한 적 없음
                    return index < 0 ? Integer.MIN_VALUE : -index;
                })
                .thenComparing(Mission::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    }
}