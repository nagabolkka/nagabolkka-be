package com.nagabolkka.backend.domain.home.service;

import com.nagabolkka.backend.domain.goal.entity.Goal;
import com.nagabolkka.backend.domain.goal.repository.GoalStepRepository;
import com.nagabolkka.backend.domain.home.dto.HomeResponse;
import com.nagabolkka.backend.domain.home.dto.HomeState;
import com.nagabolkka.backend.domain.piece.repository.PieceAcquisitionRepository;
import com.nagabolkka.backend.domain.quest.entity.DailyQuest;
import com.nagabolkka.backend.domain.quest.service.QuestService;
import com.nagabolkka.backend.domain.region.entity.Region;
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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class HomeService {

    // 마지막 접속이 3일 이상 전이면 "오랜만에 돌아옴"
    static final int WELCOME_BACK_DAYS = 3;

    // 다음 지역 제안 → 머물기를 고르면 7일 뒤 다시 제안
    static final int SUGGEST_AGAIN_DAYS = 7;
    private static final int LAST_STAGE = 5;

    private final UserRepository userRepository;
    private final UserRegionRepository userRegionRepository;
    private final PieceAcquisitionRepository pieceAcquisitionRepository;
    private final GoalStepRepository goalStepRepository;
    private final QuestService questService;
    private final Clock clock;

    // 홈 : 오늘 퀘스트가 없으면 이때 배정
    @Transactional
    public HomeResponse getHome(Long userId) {
        // 홈(4) · 퀘스트(5) 동시 호출에도 퀘스트 1개 → 사용자 행 잠금
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        if (!user.isOnboardingCompleted()) {
            throw new CustomException(ErrorCode.ONBOARDING_REQUIRED);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();

        // 오랜만에 돌아옴 : 마지막 접속 시각을 갱신하기 "전에" 판단 → 그날 첫 홈에서만
        boolean welcomeBack = isWelcomeBack(user.getLastActiveAt(), today);
        user.updateLastActiveAt(now);

        DailyQuest quest = questService.findOrAssign(user, today);

        List<UserRegion> userRegions = userRegionRepository.findByUserIdOrderByRegionStageNoAsc(userId);
        UserRegion current = userRegions.stream()
                .filter(ur -> Objects.equals(ur.getRegion().getId(), user.getCurrentRegion().getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("현재 지역 행이 없어요: user " + userId)); // 데이터 오류 → 500

        long pieceCount = pieceAcquisitionRepository.countByUserId(userId);

        Goal goal = user.getCurrentGoal();
        int goalTotal = goal == null ? 0 : (int) goalStepRepository.countByGoalId(goal.getId());
        int goalChecked = 0; // TODO M5 : user_goal_steps

        boolean rainy = false; // TODO M4 : 기상청

        return HomeResponse.of(current, pieceCount, HomeState.of(quest, welcomeBack, rainy), quest,
                userRegions, goal, goalChecked, goalTotal, nextRegionToSuggest(current, userRegions, now));
    }

    static boolean isWelcomeBack(LocalDateTime lastActiveAt, LocalDate today) {
        return lastActiveAt != null
                && ChronoUnit.DAYS.between(lastActiveAt.toLocalDate(), today) >= WELCOME_BACK_DAYS;
    }

    // 현재 지역 복원도 100% · 마지막 계단 아님 · (처음이거나 머물기 후 7일 지남) → 다음 지역
    private Region nextRegionToSuggest(UserRegion current, List<UserRegion> userRegions, LocalDateTime now) {
        int stageNo = current.getRegion().getStageNo();
        if (current.getRestoration() < 100 || stageNo >= LAST_STAGE) {
            return null;
        }
        LocalDateTime lastSuggestedAt = current.getLastSuggestedAt();
        if (lastSuggestedAt != null && lastSuggestedAt.isAfter(now.minusDays(SUGGEST_AGAIN_DAYS))) {
            return null;
        }
        return userRegions.stream()
                .filter(ur -> ur.getRegion().getStageNo() == stageNo + 1)
                .filter(ur -> ur.getStatus() == RegionStatus.LOCKED)
                .map(UserRegion::getRegion)
                .findFirst()
                .orElse(null);
    }
}