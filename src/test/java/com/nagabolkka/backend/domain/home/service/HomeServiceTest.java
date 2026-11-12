package com.nagabolkka.backend.domain.home.service;

import com.nagabolkka.backend.domain.district.entity.District;
import com.nagabolkka.backend.domain.goal.entity.Goal;
import com.nagabolkka.backend.domain.goal.repository.GoalStepRepository;
import com.nagabolkka.backend.domain.home.dto.HomeResponse;
import com.nagabolkka.backend.domain.home.dto.HomeState;
import com.nagabolkka.backend.domain.mission.entity.Mission;
import com.nagabolkka.backend.domain.piece.entity.Piece;
import com.nagabolkka.backend.domain.piece.entity.PieceType;
import com.nagabolkka.backend.domain.piece.repository.PieceAcquisitionRepository;
import com.nagabolkka.backend.domain.quest.entity.DailyQuest;
import com.nagabolkka.backend.domain.quest.entity.QuestStatus;
import com.nagabolkka.backend.domain.quest.service.QuestService;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.domain.region.entity.RegionStatus;
import com.nagabolkka.backend.domain.region.entity.UserRegion;
import com.nagabolkka.backend.domain.region.repository.UserRegionRepository;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.common.Difficulty;
import com.nagabolkka.backend.global.config.ClockConfig;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 12, 10, 0);
    private static final LocalDate TODAY = NOW.toLocalDate();
    private static final Clock CLOCK = Clock.fixed(
            ZonedDateTime.of(NOW, ClockConfig.KST).toInstant(), ClockConfig.KST);

    @Mock UserRepository userRepository;
    @Mock UserRegionRepository userRegionRepository;
    @Mock PieceAcquisitionRepository pieceAcquisitionRepository;
    @Mock GoalStepRepository goalStepRepository;
    @Mock QuestService questService;

    HomeService homeService;

    User user;
    Goal goal;
    List<Region> regions;
    List<UserRegion> userRegions;
    DailyQuest quest;

    @BeforeEach
    void setUp() {
        homeService = new HomeService(userRepository, userRegionRepository, pieceAcquisitionRepository,
                goalStepRepository, questService, CLOCK);

        regions = IntStream.rangeClosed(1, 5)
                .mapToObj(stage -> {
                    Region region = Region.of(stage, "지역" + stage, "라벨" + stage, "조각", "#00000" + stage,
                            "a.png", "b.png");
                    ReflectionTestUtils.setField(region, "id", (long) stage);
                    return region;
                })
                .toList();

        goal = Goal.of("JOB", "취업");
        ReflectionTestUtils.setField(goal, "id", 1L);

        // 3계단 출발 : 1 · 2 PASSED · 3 ACTIVE · 4 · 5 LOCKED
        user = User.createKakaoUser("홍윤지");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        user.completeOnboarding(10, regions.get(2), goal,
                District.of("성북구", new BigDecimal("37.5894"), new BigDecimal("127.0167"), 61, 127));
        userRegions = List.of(
                UserRegion.create(user, regions.get(0), RegionStatus.PASSED, null),
                UserRegion.create(user, regions.get(1), RegionStatus.PASSED, null),
                UserRegion.create(user, regions.get(2), RegionStatus.ACTIVE, NOW),
                UserRegion.create(user, regions.get(3), RegionStatus.LOCKED, null),
                UserRegion.create(user, regions.get(4), RegionStatus.LOCKED, null));

        // 오늘 퀘스트 : 지나온 1계단 미션
        Piece piece = Piece.of(regions.get(0), 1, "햇살 조각", PieceType.EASY, "하늘에 색이 돌아왔어", "힌트");
        ReflectionTestUtils.setField(piece, "id", 1L);
        Mission mission = Mission.of(regions.get(0), piece, "창문 열고 바깥 공기 3분 마시기", "설명",
                Difficulty.EASY, true, false, "내 방 창가", 3);
        quest = DailyQuest.assign(user, TODAY, mission);
    }

    private void givenHome() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(questService.findOrAssign(user, TODAY)).willReturn(quest);
        given(userRegionRepository.findByUserIdOrderByRegionStageNoAsc(USER_ID)).willReturn(userRegions);
        given(pieceAcquisitionRepository.countByUserId(USER_ID)).willReturn(2L);
        given(goalStepRepository.countByGoalId(1L)).willReturn(4L);
    }

    @Test
    @DisplayName("홈 : 현재 지역 · 모험의 길(PASSED) · 오늘 퀘스트 · 목표 · 조각 수, 마지막 접속 갱신")
    void getHome_default() {
        givenHome();

        HomeResponse response = homeService.getHome(USER_ID);

        assertThat(response.currentRegion().stageNo()).isEqualTo(3);
        assertThat(response.currentRegion().restoration()).isZero();
        assertThat(response.regions()).extracting(HomeResponse.RegionItem::status).containsExactly(
                RegionStatus.PASSED, RegionStatus.PASSED, RegionStatus.ACTIVE, RegionStatus.LOCKED, RegionStatus.LOCKED);
        assertThat(response.todayQuest().stageNo()).isEqualTo(1);   // 지나온 계단 미션
        assertThat(response.todayQuest().reward().name()).isEqualTo("햇살 조각");
        assertThat(response.pieceCount()).isEqualTo(2);
        assertThat(response.goal().name()).isEqualTo("취업");
        assertThat(response.goal().total()).isEqualTo(4);
        assertThat(response.weather().isRainy()).isFalse();
        assertThat(response.homeState()).isEqualTo(HomeState.DEFAULT); // 처음 접속 (lastActiveAt null)
        assertThat(response.nextRegionSuggestion()).isNull();
        assertThat(user.getLastActiveAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("마지막 접속 3일 전 → WELCOME_BACK / 2일 전 → DEFAULT")
    void getHome_welcomeBack() {
        givenHome();
        user.updateLastActiveAt(NOW.minusDays(3));
        assertThat(homeService.getHome(USER_ID).homeState()).isEqualTo(HomeState.WELCOME_BACK);

        user.updateLastActiveAt(NOW.minusDays(2));
        assertThat(homeService.getHome(USER_ID).homeState()).isEqualTo(HomeState.DEFAULT);
    }

    @Test
    @DisplayName("오늘 퀘스트를 완료했으면 오랜만이어도 COMPLETED 가 먼저")
    void getHome_completedFirst() {
        givenHome();
        ReflectionTestUtils.setField(quest, "status", QuestStatus.COMPLETED);
        user.updateLastActiveAt(NOW.minusDays(5));

        assertThat(homeService.getHome(USER_ID).homeState()).isEqualTo(HomeState.COMPLETED);
    }

    @Test
    @DisplayName("현재 지역 복원도 100% → 다음 지역 제안 / 머물기 3일 전이면 제안 안 함")
    void getHome_nextRegionSuggestion() {
        givenHome();
        UserRegion current = userRegions.get(2);
        ReflectionTestUtils.setField(current, "completedCount", 5);

        HomeResponse response = homeService.getHome(USER_ID);
        assertThat(response.currentRegion().restoration()).isEqualTo(100);
        assertThat(response.nextRegionSuggestion().stageNo()).isEqualTo(4);

        ReflectionTestUtils.setField(current, "lastSuggestedAt", NOW.minusDays(3));
        assertThat(homeService.getHome(USER_ID).nextRegionSuggestion()).isNull();
    }

    @Test
    @DisplayName("시작 체크 전이면 409 ONBOARDING_REQUIRED")
    void getHome_beforeOnboarding() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(User.createKakaoUser("동진")));

        assertThatThrownBy(() -> homeService.getHome(USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ONBOARDING_REQUIRED);
    }
}