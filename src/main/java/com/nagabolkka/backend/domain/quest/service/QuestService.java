package com.nagabolkka.backend.domain.quest.service;

import com.nagabolkka.backend.domain.mission.entity.Mission;
import com.nagabolkka.backend.domain.mission.entity.MissionStep;
import com.nagabolkka.backend.domain.mission.repository.MissionRepository;
import com.nagabolkka.backend.domain.mission.repository.MissionStepRepository;
import com.nagabolkka.backend.domain.piece.entity.PieceType;
import com.nagabolkka.backend.domain.piece.repository.PieceAcquisitionRepository;
import com.nagabolkka.backend.domain.quest.dto.QuestActionRequest;
import com.nagabolkka.backend.domain.quest.dto.TodayQuestResponse;
import com.nagabolkka.backend.domain.quest.entity.DailyQuest;
import com.nagabolkka.backend.domain.quest.repository.DailyQuestRepository;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.domain.region.entity.UserRegion;
import com.nagabolkka.backend.domain.region.repository.UserRegionRepository;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import com.nagabolkka.backend.global.geo.GeoPoint;
import com.nagabolkka.backend.global.weather.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private final WeatherService weatherService;
    private final QuestPlaceFinder questPlaceFinder;
    private final Clock clock;

    // 오늘 퀘스트 : 있으면 그대로, 없으면 이때 배정 (하루 1개 · 멱등)
    @Transactional
    public TodayQuestResponse getToday(Long userId, GeoPoint location) {
        User user = lockOnboardedUser(userId);
        DailyQuest quest = findOrAssign(user, LocalDate.now(clock), location);
        return toResponse(quest);
    }

    // 오늘 퀘스트 동작 : 수락 · 쪼개기 · 바꾸기 · 오늘은 쉬기 · 지나온 계단 미션 받기
    @Transactional
    public TodayQuestResponse act(Long userId, QuestActionRequest request, GeoPoint location) {
        // 버튼 연타 · 두 화면 동시 요청이 겹치지 않도록 사용자 행을 잠근다
        User user = lockOnboardedUser(userId);
        DailyQuest quest = findOrAssign(user, LocalDate.now(clock), location);
        LocalDateTime now = LocalDateTime.now(clock);

        switch (request.type()) {
            case ACCEPT -> quest.accept(now);
            case SPLIT -> quest.splitUp(now);
            case CHANGE -> {
                quest.checkChangeable(); // 끝났거나 2번 다 썼으면 미션 조회 전에 409
                List<Mission> candidates = missionRepository.findActiveByRegionIds(List.of(quest.getRegion().getId()));
                quest.change(questAssigner.pickChange(quest.getMission(), candidates));
                questPlaceFinder.fill(quest, originOf(user, location)); // 새 미션 장소
            }
            case SKIP -> {
                quest.skip();
                user.changeDifficulty(DifficultyPolicy.afterSkip(user.getDifficulty()));
            }
            case REVISIT -> {
                quest.checkChangeable(); // 끝났거나 2번 다 썼으면 409 (조회 전에)
                Region stage = passedStage(user, request.stageNo());
                List<Mission> stageMissions = missionRepository.findActiveByRegionIds(List.of(stage.getId()));
                Set<Long> foundPieceIds = new HashSet<>(pieceAcquisitionRepository.findFoundPieceIds(user.getId()));
                List<Long> recentMissionIds = dailyQuestRepository.findRecentMissionIds(user.getId(),
                        LocalDate.now(clock), PageRequest.of(0, QuestAssigner.RECENT_DAYS));
                quest.change(questAssigner.pickRevisit(stageMissions, foundPieceIds, recentMissionIds)); // 바꾸기 횟수 +1
                questPlaceFinder.fill(quest, originOf(user, location));
            }
        }

        return toResponse(quest);
    }

    // 오늘 퀘스트 있으면 그대로, 없으면 배정 — 홈에서도 사용
    public DailyQuest findOrAssign(User user, LocalDate today, GeoPoint location) {
        return dailyQuestRepository.findToday(user.getId(), today)
                .orElseGet(() -> assign(user, today, location));
    }

    // 위치 없이 (완료 처리 등) → 내 구 중심
    public DailyQuest findOrAssign(User user, LocalDate today) {
        return findOrAssign(user, today, null);
    }

    // 홈 · 퀘스트가 동시에 불려도 하나만 배정되도록 사용자 행을 잠근다
    User lockOnboardedUser(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

        if (!user.isOnboardingCompleted()) {
            throw new CustomException(ErrorCode.ONBOARDING_REQUIRED);
        }
        return user;
    }

    // 쪼갠 퀘스트만 단계 조회 · 쉼의 조각 안내는 1계단 퀘스트일 때만 조회
    private TodayQuestResponse toResponse(DailyQuest quest) {
        List<MissionStep> steps = quest.isSplit()
                ? missionStepRepository.findByMissionIdOrderByStepNoAsc(quest.getMission().getId())
                : List.of();
        boolean restPieceAvailable = quest.getRegion().getStageNo() == QuestCompleter.REST_PIECE_STAGE
                && !pieceAcquisitionRepository.existsByUserIdAndPieceType(quest.getUser().getId(), PieceType.REFLECTION);
        return TodayQuestResponse.of(quest, steps, restPieceAvailable);
    }

    // 자동 배정 : 현재 계단 미션만 (지나온 계단은 REVISIT 으로)
    private DailyQuest assign(User user, LocalDate today, GeoPoint location) {
        Region current = user.getCurrentRegion();
        List<Mission> candidates = missionRepository.findActiveByRegionIds(List.of(current.getId()));

        // 내 구에 지금 비 · 눈 → 비 오는 날 미션 (빗소리 칸 우선)
        boolean rainy = weatherService.isRainy(user.getDistrict());

        // 최근 미션 (겹치지 않게)
        List<Long> recentMissionIds = dailyQuestRepository.findRecentMissionIds(user.getId(), today,
                PageRequest.of(0, QuestAssigner.RECENT_DAYS));

        Mission mission = questAssigner.pick(current.getStageNo(), user.getDifficulty(), rainy,
                candidates, recentMissionIds);

        DailyQuest quest = DailyQuest.assign(user, today, mission);
        questPlaceFinder.fill(quest, originOf(user, location)); // 바깥 미션이면 실제 장소
        return dailyQuestRepository.save(quest);
    }

    // 앱이 보낸 위치 → 없으면 내 구 중심 (구도 없으면 null → 검색 안 함)
    private GeoPoint originOf(User user, GeoPoint location) {
        return location != null ? location : GeoPoint.of(user.getDistrict());
    }

    // 지나온 계단만 (현재 · 잠긴 · 없는 계단 → 400)
    private Region passedStage(User user, Integer stageNo) {
        if (stageNo == null || stageNo == user.getCurrentRegion().getStageNo()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        return userRegionRepository.findByUserIdOrderByRegionStageNoAsc(user.getId()).stream()
                .filter(UserRegion::isPassed)
                .map(UserRegion::getRegion)
                .filter(region -> region.getStageNo() == stageNo)
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));
    }
}