package com.nagabolkka.backend.domain.quest.service;

import com.nagabolkka.backend.domain.district.entity.District;
import com.nagabolkka.backend.domain.goal.entity.Goal;
import com.nagabolkka.backend.domain.mission.entity.Mission;
import com.nagabolkka.backend.domain.mission.repository.MissionRepository;
import com.nagabolkka.backend.domain.mission.repository.MissionStepRepository;
import com.nagabolkka.backend.domain.piece.entity.Piece;
import com.nagabolkka.backend.domain.piece.entity.PieceType;
import com.nagabolkka.backend.domain.piece.repository.PieceAcquisitionRepository;
import com.nagabolkka.backend.domain.quest.dto.TodayQuestResponse;
import com.nagabolkka.backend.domain.quest.entity.DailyQuest;
import com.nagabolkka.backend.domain.quest.entity.QuestStatus;
import com.nagabolkka.backend.domain.quest.repository.DailyQuestRepository;
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
import com.nagabolkka.backend.global.weather.WeatherService;
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
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class QuestServiceTest {

    private static final Long USER_ID = 1L;
    // KST 2026-11-12 00:30 (UTC 로는 11-11) → "오늘" 은 11-12 여야 한다
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 12);
    private static final Clock CLOCK = Clock.fixed(
            ZonedDateTime.of(2026, 11, 12, 0, 30, 0, 0, ClockConfig.KST).toInstant(), ClockConfig.KST);

    @Mock UserRepository userRepository;
    @Mock UserRegionRepository userRegionRepository;
    @Mock MissionRepository missionRepository;
    @Mock MissionStepRepository missionStepRepository;
    @Mock PieceAcquisitionRepository pieceAcquisitionRepository;
    @Mock DailyQuestRepository dailyQuestRepository;
    @Mock WeatherService weatherService;

    QuestService questService;

    User user;
    Region stage1;
    Region stage2;
    Mission stage1Easy;
    Mission stage2Easy;

    @BeforeEach
    void setUp() {
        questService = new QuestService(userRepository, userRegionRepository, missionRepository,
                missionStepRepository, pieceAcquisitionRepository, dailyQuestRepository,
                new QuestAssigner(), weatherService, CLOCK);

        stage1 = Region.of(1, "시작의 방", "집 안", "빛의 조각", "#E8C06A", "a.png", "b.png");
        stage2 = Region.of(2, "바람의 숲", "문 앞", "바람의 조각", "#9DD49A", "a.png", "b.png");
        ReflectionTestUtils.setField(stage1, "id", 1L);
        ReflectionTestUtils.setField(stage2, "id", 2L);

        stage1Easy = mission(stage1, 11L, "창문 열고 바깥 공기 3분 마시기", true);
        stage2Easy = mission(stage2, 21L, "집 앞 골목 한 바퀴, 10분 산책", false);

        user = User.createKakaoUser("홍윤지");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        District district = District.of("성북구", new BigDecimal("37.5894"), new BigDecimal("127.0167"), 61, 127);
        // 2계단 출발 → 1계단은 지나온 곳
        user.completeOnboarding(7, stage2, Goal.of("JOB", "취업"), district);
    }

    private Mission mission(Region region, long pieceId, String title, boolean indoor) {
        Piece piece = Piece.of(region, 1, "조각" + pieceId, PieceType.EASY, "복원 문구", "힌트");
        ReflectionTestUtils.setField(piece, "id", pieceId);
        return Mission.of(region, piece, title, "설명", Difficulty.EASY, indoor, false, "장소", 5);
    }

    @Test
    @DisplayName("오늘 퀘스트가 이미 있으면 그대로 (새로 배정 안 함)")
    void getToday_existing() {
        DailyQuest existing = DailyQuest.assign(user, TODAY, stage2Easy);
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(dailyQuestRepository.findToday(USER_ID, TODAY)).willReturn(Optional.of(existing));

        TodayQuestResponse response = questService.getToday(USER_ID);

        assertThat(response.title()).isEqualTo("집 앞 골목 한 바퀴, 10분 산책");
        then(dailyQuestRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("없으면 배정 : 쉬운 날 + 1계단 지나옴 → 1계단 미션, KST 날짜로 저장")
    void getToday_assign() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(dailyQuestRepository.findToday(USER_ID, TODAY)).willReturn(Optional.empty());
        given(userRegionRepository.findByUserIdOrderByRegionStageNoAsc(user.getId())).willReturn(List.of(
                UserRegion.create(user, stage1, RegionStatus.PASSED, null),
                UserRegion.create(user, stage2, RegionStatus.ACTIVE, null)));
        given(missionRepository.findActiveByRegionIds(anyCollection())).willReturn(List.of(stage1Easy, stage2Easy));
        given(pieceAcquisitionRepository.findFoundPieceIds(user.getId())).willReturn(List.of());
        given(dailyQuestRepository.save(any(DailyQuest.class))).willAnswer(inv -> inv.getArgument(0));

        TodayQuestResponse response = questService.getToday(USER_ID);

        assertThat(response.questDate()).isEqualTo(TODAY);
        assertThat(response.status()).isEqualTo(QuestStatus.ASSIGNED);
        assertThat(response.region().stageNo()).isEqualTo(1); // 지나온 계단 미션
        assertThat(response.reward().pieceId()).isEqualTo(11L);
        assertThat(response.place().distanceM()).isZero();    // 실내
        assertThat(response.steps()).isEmpty();                // 쪼개기 전
        assertThat(response.changeRemaining()).isEqualTo(2);
    }

    @Test
    @DisplayName("시작 체크 전이면 409 ONBOARDING_REQUIRED")
    void getToday_beforeOnboarding() {
        User newUser = User.createKakaoUser("동진");
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(newUser));

        assertThatThrownBy(() -> questService.getToday(USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ONBOARDING_REQUIRED);
    }

    @Test
    @DisplayName("사용자가 없으면(탈퇴) 401 UNAUTHORIZED")
    void getToday_userNotFound() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> questService.getToday(USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("쉼의 조각 안내 : 1계단 퀘스트 · 아직 안 받음 → true, 받은 뒤 → false")
    void restPieceAvailable_stage1() {
        DailyQuest stage1Quest = DailyQuest.assign(user, TODAY, stage1Easy);
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(dailyQuestRepository.findToday(USER_ID, TODAY)).willReturn(Optional.of(stage1Quest));
        given(pieceAcquisitionRepository.existsByUserIdAndPieceType(USER_ID, PieceType.REFLECTION))
                .willReturn(false, true); // 첫 조회 : 안 받음 → 두 번째 : 받음

        assertThat(questService.getToday(USER_ID).restPieceAvailable()).isTrue();
        assertThat(questService.getToday(USER_ID).restPieceAvailable()).isFalse();
    }

    @Test
    @DisplayName("쉼의 조각 안내 : 2계단 퀘스트 → false (조각 기록 조회 안 함)")
    void restPieceAvailable_otherStage() {
        DailyQuest stage2Quest = DailyQuest.assign(user, TODAY, stage2Easy);
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(dailyQuestRepository.findToday(USER_ID, TODAY)).willReturn(Optional.of(stage2Quest));

        assertThat(questService.getToday(USER_ID).restPieceAvailable()).isFalse();
        then(pieceAcquisitionRepository).should(never()).existsByUserIdAndPieceType(any(), any());
    }
}