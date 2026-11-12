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

    public Mission pick(int currentStageNo, Difficulty difficulty, boolean rainy,
                        List<Mission> candidates, Set<Integer> passedStageNos, Set<Long> foundPieceIds) {

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

        // 2. 쉬운 날 → 지나온 계단의 못 찾은 조각
        if (difficulty == Difficulty.EASY) {
            Optional<Mission> passedMission = candidates.stream()
                    .filter(m -> passedStageNos.contains(m.getRegion().getStageNo()))
                    .filter(m -> !m.isRainyOnly())
                    .filter(m -> !foundPieceIds.contains(m.getRewardPiece().getId()))
                    .min(Comparator.comparingInt((Mission m) -> m.getRegion().getStageNo())
                            .thenComparing(Mission::getDifficulty));
            if (passedMission.isPresent()) {
                return passedMission.get();
            }
        }

        // 3. 현재 계단 · 오늘 난이도 (없으면 현재 계단 쉬운 미션)
        return current.stream()
                .filter(m -> !m.isRainyOnly())
                .filter(m -> m.getDifficulty() == difficulty)
                .findFirst()
                .or(() -> current.stream()
                        .filter(m -> !m.isRainyOnly())
                        .filter(m -> m.getDifficulty() == Difficulty.EASY)
                        .findFirst())
                .orElseThrow(() -> new IllegalStateException("미션 시드가 없어요: " + currentStageNo + "계단")); // 시드 누락 = 버그 → 500
    }

    // 바꾸기는 1 · 2칸 미션끼리만 (3칸 도전 · 빗소리 칸 제외)
    private static final int CHANGE_MAX_SLOT = 2;

    // 바꾸기 : 같은 지역 1 · 2칸 중 지금 미션의 다음 칸, 마지막이면 처음 칸
    public Mission pickChange(Mission current, List<Mission> candidates) {
        int currentStageNo = current.getRegion().getStageNo();
        int currentSlot = current.getRewardPiece().getSlotNo();

        List<Mission> options = candidates.stream()
                .filter(m -> m.getRegion().getStageNo() == currentStageNo)
                .filter(m -> !m.isRainyOnly())
                .filter(m -> m.getRewardPiece().getSlotNo() <= CHANGE_MAX_SLOT)
                .filter(m -> !m.getId().equals(current.getId()))
                .sorted(Comparator.comparingInt((Mission m) -> m.getRewardPiece().getSlotNo()))
                .toList();

        return options.stream()
                .filter(m -> m.getRewardPiece().getSlotNo() > currentSlot)
                .findFirst()
                .or(() -> options.stream().findFirst())
                .orElseThrow(() -> new IllegalStateException("바꿀 미션 시드가 없어요: " + currentStageNo + "계단")); // 시드 누락 = 버그 → 500
    }
}