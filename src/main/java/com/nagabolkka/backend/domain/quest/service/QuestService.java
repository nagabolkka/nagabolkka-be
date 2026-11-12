package com.nagabolkka.backend.domain.quest.service;

import com.nagabolkka.backend.domain.mission.entity.Mission;
import com.nagabolkka.backend.domain.mission.entity.MissionStep;
import com.nagabolkka.backend.domain.mission.repository.MissionRepository;
import com.nagabolkka.backend.domain.mission.repository.MissionStepRepository;
import com.nagabolkka.backend.domain.piece.repository.PieceAcquisitionRepository;
import com.nagabolkka.backend.domain.quest.dto.TodayQuestResponse;
import com.nagabolkka.backend.domain.quest.entity.DailyQuest;
import com.nagabolkka.backend.domain.quest.repository.DailyQuestRepository;
import com.nagabolkka.backend.domain.region.entity.RegionStatus;
import com.nagabolkka.backend.domain.region.entity.UserRegion;
import com.nagabolkka.backend.domain.region.repository.UserRegionRepository;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestService {

    private final UserRepository userRepository;
    private final UserRegionRepository userRegionRepository;
    private final MissionRepository missionRepository;
    private final MissionStepRepository missionStepRepository;
    private final PieceAcquisitionRepository pieceAcquisitionRepository;
    private final DailyQuestRepository dailyQuestRepository;
    private final QuestAssigner questAssigner;
    private final Clock clock;

    // 오늘 퀘스트 : 있으면 그대로, 없으면 이때 배정 (하루 1개 · 멱등)
    @Transactional
    public TodayQuestResponse getToday(Long userId) {
        // 홈 · 퀘스트가 동시에 불려도 하나만 배정되도록 사용자 행을 잠근다
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

        if (!user.isOnboardingCompleted()) {
            throw new CustomException(ErrorCode.ONBOARDING_REQUIRED);
        }

        LocalDate today = LocalDate.now(clock);
        DailyQuest quest = dailyQuestRepository.findToday(userId, today)
                .orElseGet(() -> assign(user, today));

        List<MissionStep> steps = quest.isSplit()
                ? missionStepRepository.findByMissionIdOrderByStepNoAsc(quest.getMission().getId())
                : List.of();

        return TodayQuestResponse.of(quest, steps);
    }

    private DailyQuest assign(User user, LocalDate today) {
        // 지나온 계단
        List<UserRegion> userRegions = userRegionRepository.findByUserIdOrderByRegionStageNoAsc(user.getId());
        Set<Integer> passedStageNos = userRegions.stream()
                .filter(ur -> ur.getStatus() == RegionStatus.PASSED)
                .map(ur -> ur.getRegion().getStageNo())
                .collect(Collectors.toSet());

        // 후보 미션 : 현재 + 지나온 계단
        Set<Long> regionIds = userRegions.stream()
                .filter(ur -> ur.getStatus() == RegionStatus.PASSED)
                .map(ur -> ur.getRegion().getId())
                .collect(Collectors.toCollection(HashSet::new));
        regionIds.add(user.getCurrentRegion().getId());
        List<Mission> candidates = missionRepository.findActiveByRegionIds(regionIds);

        Set<Long> foundPieceIds = new HashSet<>(pieceAcquisitionRepository.findFoundPieceIds(user.getId()));

        boolean rainy = false;

        Mission mission = questAssigner.pick(user.getCurrentRegion().getStageNo(), user.getDifficulty(),
                rainy, candidates, passedStageNos, foundPieceIds);

        return dailyQuestRepository.save(DailyQuest.assign(user, today, mission));
    }
}