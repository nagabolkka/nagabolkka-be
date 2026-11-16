package com.nagabolkka.backend.domain.quest.service;

import com.nagabolkka.backend.domain.district.entity.District;
import com.nagabolkka.backend.domain.goal.entity.Goal;
import com.nagabolkka.backend.domain.mission.entity.Mission;
import com.nagabolkka.backend.domain.mission.entity.MissionStep;
import com.nagabolkka.backend.domain.mission.repository.MissionRepository;
import com.nagabolkka.backend.domain.mission.repository.MissionStepRepository;
import com.nagabolkka.backend.domain.piece.entity.Piece;
import com.nagabolkka.backend.domain.piece.entity.PieceType;
import com.nagabolkka.backend.domain.piece.repository.PieceAcquisitionRepository;
import com.nagabolkka.backend.domain.quest.dto.QuestActionRequest;
import com.nagabolkka.backend.domain.quest.dto.QuestActionType;
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
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class QuestActionServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 13);
    private static final Clock CLOCK = Clock.fixed(
            ZonedDateTime.of(2026, 11, 13, 10, 0, 0, 0, ClockConfig.KST).toInstant(), ClockConfig.KST);

    @Mock UserRepository userRepository;
    @Mock UserRegionRepository userRegionRepository;
    @Mock MissionRepository missionRepository;
    @Mock MissionStepRepository missionStepRepository;
    @Mock PieceAcquisitionRepository pieceAcquisitionRepository;
    @Mock DailyQuestRepository dailyQuestRepository;
    @Mock WeatherService weatherService;
    @Mock QuestPlaceFinder questPlaceFinder;
    @Mock PlaceQuestFactory placeQuestFactory;

    QuestService questService;

    User user;
    Region stage2;
    Mission slot1;     // 바람의 숲 쉬움 (오늘 퀘스트)
    Mission slot2;     // 보통 → 쉬움 바꾸기 대상 아님
    Mission slot3;     // 도전 → 쉬움 바꾸기 대상 아님
    Mission rainy;     // 빗소리 → 바꾸기 대상 아님
    DailyQuest quest;

    @BeforeEach
    void setUp() {
        questService = new QuestService(userRepository, userRegionRepository, missionRepository,
                missionStepRepository, pieceAcquisitionRepository, dailyQuestRepository,
                new QuestAssigner(), weatherService, questPlaceFinder, placeQuestFactory, CLOCK);

        stage2 = Region.of(2, "바람의 숲", "문 앞", "바람의 조각", "#9DD49A", "a.png", "b.png");
        ReflectionTestUtils.setField(stage2, "id", 2L);

        slot1 = mission(201L, 1, PieceType.EASY, "현관문 열고 1분 서 있기");
        slot2 = mission(202L, 2, PieceType.NORMAL, "집 앞 골목 한 바퀴, 10분 산책");
        slot3 = mission(203L, 3, PieceType.CHALLENGE, "편의점에서 물 한 병 사기");
        rainy = mission(204L, 4, PieceType.RAINY, "창가에서 빗소리 듣기");

        user = User.createKakaoUser("홍윤지");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        District district = District.of("성북구", new BigDecimal("37.5894"), new BigDecimal("127.0167"), 61, 127);
        user.completeOnboarding(5, stage2, Goal.of("JOB", "취업"), district);

        quest = DailyQuest.assign(user, TODAY, slot1);
    }

    // 조각 종류로 난이도 (쉬움 · 빗소리 = EASY)
    private Mission mission(long id, int slotNo, PieceType type, String title) {
        Piece piece = Piece.of(stage2, slotNo, "조각" + slotNo, type, "복원 문구", "힌트");
        ReflectionTestUtils.setField(piece, "id", id);
        Difficulty difficulty = switch (type) {
            case NORMAL -> Difficulty.NORMAL;
            case CHALLENGE -> Difficulty.CHALLENGE;
            default -> Difficulty.EASY;
        };
        Mission mission = Mission.of(stage2, piece, title, "설명", difficulty, true, false, "장소", 5);
        ReflectionTestUtils.setField(mission, "id", id);
        return mission;
    }

    private void givenTodayQuest() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(dailyQuestRepository.findToday(USER_ID, TODAY)).willReturn(Optional.of(quest));
    }

    private TodayQuestResponse act(QuestActionType type) {
        return questService.act(USER_ID, new QuestActionRequest(type), null);
    }

    @Test
    @DisplayName("ACCEPT → IN_PROGRESS (쪼개기 전이라 단계 조회 안 함)")
    void accept() {
        givenTodayQuest();

        TodayQuestResponse response = act(QuestActionType.ACCEPT);

        assertThat(response.status()).isEqualTo(QuestStatus.IN_PROGRESS);
        assertThat(response.steps()).isEmpty();
        then(missionStepRepository).should(never()).findByMissionIdOrderByStepNoAsc(any());
    }

    @Test
    @DisplayName("SPLIT → isSplit · steps 3개 · IN_PROGRESS")
    void split() {
        givenTodayQuest();
        given(missionStepRepository.findByMissionIdOrderByStepNoAsc(201L)).willReturn(List.of(
                MissionStep.of(slot1, 1, "현관까지 걸어가기"),
                MissionStep.of(slot1, 2, "문 손잡이 잡아보기"),
                MissionStep.of(slot1, 3, "문 열고 1분 서 있기")));

        TodayQuestResponse response = act(QuestActionType.SPLIT);

        assertThat(response.isSplit()).isTrue();
        assertThat(response.status()).isEqualTo(QuestStatus.IN_PROGRESS);
        assertThat(response.steps()).extracting(TodayQuestResponse.StepInfo::stepNo).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("CHANGE : 같은 계단 · 같은 난이도(쉬움) 안에서 다음 미션 (보통 · 도전 · 빗소리 제외), 3번째는 409 · 미션 조회도 안 함")
    void change() {
        givenTodayQuest();
        Mission easy2 = mission(205L, 1, PieceType.EASY, "집 앞에서 하늘 사진 한 장 찍기");
        Mission easy3 = mission(206L, 1, PieceType.EASY, "우리 건물 한 바퀴 돌기");
        given(missionRepository.findActiveByRegionIds(anyCollection()))
                .willReturn(List.of(slot1, slot2, slot3, rainy, easy2, easy3));

        TodayQuestResponse first = act(QuestActionType.CHANGE);
        assertThat(first.title()).isEqualTo("집 앞에서 하늘 사진 한 장 찍기");
        assertThat(first.difficulty()).isEqualTo(Difficulty.EASY);
        assertThat(first.status()).isEqualTo(QuestStatus.ASSIGNED);
        assertThat(first.changeRemaining()).isEqualTo(1);

        TodayQuestResponse second = act(QuestActionType.CHANGE);
        assertThat(second.title()).isEqualTo("우리 건물 한 바퀴 돌기");
        assertThat(second.changeRemaining()).isZero();

        assertThatThrownBy(() -> act(QuestActionType.CHANGE))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.QUEST_CHANGE_LIMIT);
        then(missionRepository).should(times(2)).findActiveByRegionIds(anyCollection());
    }

    @Test
    @DisplayName("REVISIT : 지나온 1계단 → 못 찾은 조각 중 가장 쉬운 미션 · 바꾸기 1번 사용")
    void revisit() {
        givenTodayQuest();
        Region stage1 = Region.of(1, "시작의 방", "집 안", "빛의 조각", "#E8C06A", "a.png", "b.png");
        ReflectionTestUtils.setField(stage1, "id", 1L);
        Piece sun = Piece.of(stage1, 1, "햇살 조각", PieceType.EASY, "복원 문구", "힌트");
        ReflectionTestUtils.setField(sun, "id", 11L);
        Piece morning = Piece.of(stage1, 2, "아침 조각", PieceType.NORMAL, "복원 문구", "힌트");
        ReflectionTestUtils.setField(morning, "id", 12L);
        Mission easy = Mission.of(stage1, sun, "창문 열고 바깥 공기 3분 마시기", "설명", Difficulty.EASY, true, false, "내 방 창가", 3);
        ReflectionTestUtils.setField(easy, "id", 101L);
        Mission normal = Mission.of(stage1, morning, "이불 개고 침대 위 정리하기", "설명", Difficulty.NORMAL, true, false, "내 방", 5);
        ReflectionTestUtils.setField(normal, "id", 102L);

        given(userRegionRepository.findByUserIdOrderByRegionStageNoAsc(USER_ID)).willReturn(List.of(
                UserRegion.create(user, stage1, RegionStatus.PASSED, null),
                UserRegion.create(user, stage2, RegionStatus.ACTIVE, null)));
        given(missionRepository.findActiveByRegionIds(List.of(1L))).willReturn(List.of(easy, normal));
        given(pieceAcquisitionRepository.findFoundPieceIds(USER_ID)).willReturn(List.of(11L)); // 햇살 조각은 이미 찾음

        TodayQuestResponse res = questService.act(USER_ID, new QuestActionRequest(QuestActionType.REVISIT, 1), null);

        assertThat(res.region().stageNo()).isEqualTo(1);
        assertThat(res.title()).isEqualTo("이불 개고 침대 위 정리하기"); // 아침 조각 = 못 찾은 조각 중 가장 쉬움
        assertThat(res.status()).isEqualTo(QuestStatus.ASSIGNED);
        assertThat(res.changeRemaining()).isEqualTo(1);
    }

    @Test
    @DisplayName("REVISIT : 현재 계단 · stageNo 없음 · 잠긴 계단 → 400 INVALID_INPUT")
    void revisit_notPassed() {
        givenTodayQuest();
        given(userRegionRepository.findByUserIdOrderByRegionStageNoAsc(USER_ID)).willReturn(List.of(
                UserRegion.create(user, stage2, RegionStatus.ACTIVE, null)));

        assertThatThrownBy(() -> questService.act(USER_ID, new QuestActionRequest(QuestActionType.REVISIT, 2), null))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);   // 현재 계단 (조회 전에 막힘)
        assertThatThrownBy(() -> questService.act(USER_ID, new QuestActionRequest(QuestActionType.REVISIT, null), null))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);   // stageNo 없음
        assertThatThrownBy(() -> questService.act(USER_ID, new QuestActionRequest(QuestActionType.REVISIT, 3), null))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);   // 잠긴 계단
    }

    @Test
    @DisplayName("SKIP → SKIPPED, 그 뒤 ACCEPT 는 409 QUEST_ALREADY_DONE")
    void skip_thenAccept() {
        givenTodayQuest();

        TodayQuestResponse response = act(QuestActionType.SKIP);
        assertThat(response.status()).isEqualTo(QuestStatus.SKIPPED);

        assertThatThrownBy(() -> act(QuestActionType.ACCEPT))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.QUEST_ALREADY_DONE);
    }

    @Test
    @DisplayName("시작 체크 전이면 409 ONBOARDING_REQUIRED (퀘스트 조회 안 함)")
    void beforeOnboarding() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(User.createKakaoUser("동진")));

        assertThatThrownBy(() -> act(QuestActionType.ACCEPT))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ONBOARDING_REQUIRED);
        then(dailyQuestRepository).should(never()).findToday(any(), any());
    }

    @Test
    @DisplayName("SKIP → 다음 날 난이도 한 단계 내림 (NORMAL → EASY)")
    void skip_lowersDifficulty() {
        givenTodayQuest();
        ReflectionTestUtils.setField(user, "difficulty", Difficulty.NORMAL);

        act(QuestActionType.SKIP);

        assertThat(user.getDifficulty()).isEqualTo(Difficulty.EASY);
    }
}