package com.nagabolkka.backend.domain.quest.service;

import com.nagabolkka.backend.domain.district.entity.District;
import com.nagabolkka.backend.domain.goal.entity.Goal;
import com.nagabolkka.backend.domain.mission.entity.Mission;
import com.nagabolkka.backend.domain.mission.repository.MissionRepository;
import com.nagabolkka.backend.domain.mission.repository.MissionStepRepository;
import com.nagabolkka.backend.domain.piece.entity.Piece;
import com.nagabolkka.backend.domain.piece.entity.PieceType;
import com.nagabolkka.backend.domain.piece.repository.PieceAcquisitionRepository;
import com.nagabolkka.backend.domain.practice.repository.PracticeSessionRepository;
import com.nagabolkka.backend.domain.quest.dto.QuestActionRequest;
import com.nagabolkka.backend.domain.quest.dto.QuestActionType;
import com.nagabolkka.backend.domain.quest.dto.TodayQuestResponse;
import com.nagabolkka.backend.domain.quest.entity.DailyQuest;
import com.nagabolkka.backend.domain.quest.repository.DailyQuestRepository;
import com.nagabolkka.backend.domain.region.entity.Region;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class QuestDifficultyServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 25);
    private static final Clock CLOCK = Clock.fixed(
            ZonedDateTime.of(2026, 11, 25, 10, 0, 0, 0, ClockConfig.KST).toInstant(), ClockConfig.KST);

    @Mock UserRepository userRepository;
    @Mock UserRegionRepository userRegionRepository;
    @Mock MissionRepository missionRepository;
    @Mock MissionStepRepository missionStepRepository;
    @Mock PieceAcquisitionRepository pieceAcquisitionRepository;
    @Mock DailyQuestRepository dailyQuestRepository;
    @Mock WeatherService weatherService;
    @Mock QuestPlaceFinder questPlaceFinder;
    @Mock PlaceQuestFactory placeQuestFactory;
    @Mock PracticeSessionRepository practiceSessionRepository;

    QuestService questService;

    User user;
    Region stage2;
    Mission easy;       // 오늘 퀘스트 (쉬움)
    Mission normal;     // 보통
    Mission challenge;  // 도전
    DailyQuest quest;

    @BeforeEach
    void setUp() {
        questService = new QuestService(userRepository, userRegionRepository, missionRepository,
                missionStepRepository, pieceAcquisitionRepository, dailyQuestRepository,
                new QuestAssigner(), weatherService, questPlaceFinder, placeQuestFactory,
                practiceSessionRepository, CLOCK);

        stage2 = Region.of(2, "바람의 숲", "문 앞", "바람의 조각", "#9DD49A", "a.png", "b.png");
        ReflectionTestUtils.setField(stage2, "id", 2L);

        easy = mission(201L, 1, PieceType.EASY, Difficulty.EASY, "현관문 열고 1분 서 있기");
        normal = mission(202L, 2, PieceType.NORMAL, Difficulty.NORMAL, "집 앞 골목 한 바퀴, 10분 산책");
        challenge = mission(203L, 3, PieceType.CHALLENGE, Difficulty.CHALLENGE, "편의점에서 물 한 병 사기");

        user = User.createKakaoUser("홍윤지");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        District district = District.of("성북구", new BigDecimal("37.5894"), new BigDecimal("127.0167"), 61, 127);
        user.completeOnboarding(5, stage2, Goal.of("JOB", "취업"), district);

        quest = DailyQuest.assign(user, TODAY, easy);
    }

    private Mission mission(long id, int slotNo, PieceType type, Difficulty difficulty, String title) {
        Piece piece = Piece.of(stage2, slotNo, "조각" + slotNo, type, "복원 문구", "힌트");
        ReflectionTestUtils.setField(piece, "id", id);
        Mission mission = Mission.of(stage2, piece, title, "설명", difficulty, true, false, "장소", 5);
        ReflectionTestUtils.setField(mission, "id", id);
        return mission;
    }

    private void givenTodayQuest() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(dailyQuestRepository.findToday(USER_ID, TODAY)).willReturn(Optional.of(quest));
    }

    private void givenCandidates() {
        given(missionRepository.findActiveByRegionIds(List.of(2L))).willReturn(List.of(easy, normal, challenge));
        given(dailyQuestRepository.findRecentMissionIds(any(), any(), any())).willReturn(List.of());
    }

    @Test
    @DisplayName("HARDER : 쉬움 → 보통 미션 · 난이도 횟수 0 · 바꾸기 2번 그대로 · 내일 난이도 (users.difficulty) 그대로")
    void harder() {
        givenTodayQuest();
        givenCandidates();
        Difficulty before = user.getDifficulty();

        TodayQuestResponse response = questService.act(USER_ID, new QuestActionRequest(QuestActionType.HARDER), null);

        assertThat(response.difficulty()).isEqualTo(Difficulty.NORMAL);
        assertThat(response.title()).isEqualTo("집 앞 골목 한 바퀴, 10분 산책");
        assertThat(response.difficultyChangeRemaining()).isZero();
        assertThat(response.changeRemaining()).isEqualTo(2);
        assertThat(user.getDifficulty()).isEqualTo(before);
        then(questPlaceFinder).should().fill(any(), any()); // 새 미션 장소 찾기
    }

    @Test
    @DisplayName("EASIER : 보통 → 쉬움 미션")
    void easier() {
        quest = DailyQuest.assign(user, TODAY, normal);
        givenTodayQuest();
        givenCandidates();

        TodayQuestResponse response = questService.act(USER_ID, new QuestActionRequest(QuestActionType.EASIER), null);

        assertThat(response.difficulty()).isEqualTo(Difficulty.EASY);
        assertThat(response.title()).isEqualTo("현관문 열고 1분 서 있기");
    }

    @Test
    @DisplayName("쉬움에서 EASIER → 409 LOCKED, 미션 조회 안 함")
    void easier_atEasy() {
        givenTodayQuest();

        assertThatThrownBy(() -> questService.act(USER_ID, new QuestActionRequest(QuestActionType.EASIER), null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.QUEST_DIFFICULTY_LOCKED);
        then(missionRepository).should(never()).findActiveByRegionIds(any());
    }

    @Test
    @DisplayName("도전에서 HARDER → 409 LOCKED")
    void harder_atChallenge() {
        quest = DailyQuest.assign(user, TODAY, challenge);
        givenTodayQuest();

        assertThatThrownBy(() -> questService.act(USER_ID, new QuestActionRequest(QuestActionType.HARDER), null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.QUEST_DIFFICULTY_LOCKED);
    }

    @Test
    @DisplayName("오늘 이미 바꿨으면 → 409 LIMIT, 미션 조회 안 함")
    void limit() {
        quest.changeDifficulty(normal);
        givenTodayQuest();

        assertThatThrownBy(() -> questService.act(USER_ID, new QuestActionRequest(QuestActionType.HARDER), null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.QUEST_DIFFICULTY_LIMIT);
        then(missionRepository).should(never()).findActiveByRegionIds(any());
    }

    @Test
    @DisplayName("수락한 퀘스트 → 409 LOCKED")
    void locked_inProgress() {
        quest.accept(TODAY.atTime(9, 0));
        givenTodayQuest();

        assertThatThrownBy(() -> questService.act(USER_ID, new QuestActionRequest(QuestActionType.HARDER), null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.QUEST_DIFFICULTY_LOCKED);
    }
}