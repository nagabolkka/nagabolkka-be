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
import com.nagabolkka.backend.domain.practice.entity.PracticeStatus;
import com.nagabolkka.backend.domain.practice.repository.PracticeSessionRepository;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QuestService {

    // 지금 위치 ↔ 퀘스트 장소가 검색 반경의 이 배수보다 멀면 다시 찾기 (2계단 700m → 1.4km)
    static final int MOVED_FAR_FACTOR = 2;

    private final UserRepository userRepository;
    private final UserRegionRepository userRegionRepository;
    private final MissionRepository missionRepository;
    private final MissionStepRepository missionStepRepository;
    private final PieceAcquisitionRepository pieceAcquisitionRepository;
    private final DailyQuestRepository dailyQuestRepository;
    private final QuestAssigner questAssigner;
    private final WeatherService weatherService;
    private final QuestPlaceFinder questPlaceFinder;
    private final PlaceQuestFactory placeQuestFactory;
    private final PracticeSessionRepository practiceSessionRepository;
    private final Clock clock;

    // 오늘 퀘스트 : 있으면 그대로 (수락 전 · 멀리 이동했으면 장소만 다시), 없으면 이때 배정 (하루 1개 · 멱등)
    @Transactional
    public TodayQuestResponse getToday(Long userId, GeoPoint location) {
        User user = lockOnboardedUser(userId);
        DailyQuest quest = findOrAssignNearby(user, LocalDate.now(clock), location);
        return toResponse(quest);
    }

    // 오늘 퀘스트 동작 : 수락 · 쪼개기 · 바꾸기 · 오늘은 쉬기 · 지나온 계단 미션 받기
    // (장소 다시 찾기 X → 버튼 누르는 순간 장소가 바뀌지 않게)
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
                // 퀘스트 계단 기준 (연습 후 미션이면 미션 계단과 다름)
                quest.change(questAssigner.pickChange(quest.getMission(), quest.getRegion().getStageNo(), candidates));
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

            case PRACTICE_MISSION -> {
                Mission follow = practiceFollowMission(user, request.missionId()); // 오늘 끝낸 연습이 제안한 것만
                if (!quest.isPracticeMission(follow.getId())) { // 두 번 눌러도 진행 상태 유지
                    quest.replaceWithPractice(follow, user.getCurrentRegion()); // 끝난 퀘스트 → 409
                    questPlaceFinder.fill(quest, originOf(user, location)); // 첫 방문 미션 → 근처 도서관
                }
            }
        }

        return toResponse(quest);
    }

    // 오늘 퀘스트 있으면 그대로, 없으면 배정 — 동작 · 완료에서 사용
    public DailyQuest findOrAssign(User user, LocalDate today, GeoPoint location) {
        return dailyQuestRepository.findToday(user.getId(), today)
                .orElseGet(() -> assign(user, today, location));
    }

    // 위치 없이 (완료 처리 등) → 내 구 중심
    public DailyQuest findOrAssign(User user, LocalDate today) {
        return findOrAssign(user, today, null);
    }

    // 홈 · 오늘 퀘스트 조회용 : 있으면 (수락 전 · 멀리 이동했으면) 장소를 지금 위치로 다시 찾기, 없으면 배정
    public DailyQuest findOrAssignNearby(User user, LocalDate today, GeoPoint location) {
        return dailyQuestRepository.findToday(user.getId(), today)
                .map(quest -> refindIfMovedFar(quest, location))
                .orElseGet(() -> assign(user, today, location));
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

    // 수락 전 · 담기 아님 · 찾은 장소 있음 → 지금 위치가 장소에서 (검색 반경 × 2) 보다 멀면 지금 위치로 다시
    private DailyQuest refindIfMovedFar(DailyQuest quest, GeoPoint location) {
        if (location == null || !quest.canRefindPlace()) {
            return quest;
        }
        int limit = QuestPlaceFinder.radiusOf(quest.getRegion().getStageNo()) * MOVED_FAR_FACTOR;
        if (location.distanceTo(quest.placePoint()) > limit) {
            questPlaceFinder.fill(quest, location); // 못 찾으면 예전 장소 그대로 (트랜잭션 안 → 변경 감지로 저장)
        }
        return quest;
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

        GeoPoint origin = originOf(user, location);

        // 1순위 : 동네 지도에서 담아 둔 장소 (기간 끝났으면 취소 후 평소 배정)
        Optional<DailyQuest> savedPlaceQuest = placeQuestFactory.assignWaiting(user, today, origin);
        if (savedPlaceQuest.isPresent()) {
            return savedPlaceQuest.get();
        }

        Region current = user.getCurrentRegion();
        List<Mission> candidates = missionRepository.findActiveByRegionIds(List.of(current.getId()));

        // 지금 위치에 비 · 눈 → 비 오는 날 미션 (빗소리 칸 우선)
        boolean rainy = weatherService.isRainy(user.getDistrict(), location);

        // 최근 미션 (겹치지 않게)
        List<Long> recentMissionIds = dailyQuestRepository.findRecentMissionIds(user.getId(), today,
                PageRequest.of(0, QuestAssigner.RECENT_DAYS));

        Mission mission = questAssigner.pick(current.getStageNo(), user.getDifficulty(), rainy,
                candidates, recentMissionIds);

        DailyQuest quest = DailyQuest.assign(user, today, mission);
        questPlaceFinder.fill(quest, origin); // 바깥 미션이면 실제 장소
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

    // PRACTICE_MISSION : 오늘 COMPLETED 한 연습 세션의 시나리오가 제안한 미션만 (다른 값 · 어제 연습 → 404)
    private Mission practiceFollowMission(User user, Long missionId) {
        if (missionId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        LocalDateTime todayStart = LocalDate.now(clock).atStartOfDay();
        if (!practiceSessionRepository.existsFollowMission(user.getId(), missionId, PracticeStatus.COMPLETED, todayStart)) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
        return missionRepository.findById(missionId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
    }
}