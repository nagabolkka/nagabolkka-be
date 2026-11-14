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

    QuestService questService;

    User user;
    Region stage2;
    Mission slot1;     // 바람의 숲 1칸
    Mission slot2;     // 바람의 숲 2칸
    Mission slot3;     // 3칸 도전 → 바꾸기 대상 아님
    Mission rainy;     // 빗소리 칸 → 바꾸기 대상 아님
    DailyQuest quest;

    @BeforeEach
    void setUp() {
        questService = new QuestService(userRepository, userRegionRepository, missionRepository,
                missionStepRepository, pieceAcquisitionRepository, dailyQuestRepository,
                new QuestAssigner(), CLOCK);

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

    private Mission mission(long id, int slotNo, PieceType type, String title) {
        Piece piece = Piece.of(stage2, slotNo, "조각" + slotNo, type, "복원 문구", "힌트");
        ReflectionTestUtils.setField(piece, "id", id);
        Mission mission = Mission.of(stage2, piece, title, "설명", Difficulty.EASY, true, false, "장소", 5);
        ReflectionTestUtils.setField(mission, "id", id);
        return mission;
    }

    private void givenTodayQuest() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(dailyQuestRepository.findToday(USER_ID, TODAY)).willReturn(Optional.of(quest));
    }

    private TodayQuestResponse act(QuestActionType type) {
        return questService.act(USER_ID, new QuestActionRequest(type));
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
    @DisplayName("CHANGE : 1칸 → 2칸 → 1칸 (도전 · 빗소리 칸 제외), 3번째는 409 · 미션 조회도 안 함")
    void change() {
        givenTodayQuest();
        given(missionRepository.findActiveByRegionIds(anyCollection()))
                .willReturn(List.of(slot1, slot2, slot3, rainy));

        TodayQuestResponse first = act(QuestActionType.CHANGE);
        assertThat(first.title()).isEqualTo("집 앞 골목 한 바퀴, 10분 산책");
        assertThat(first.status()).isEqualTo(QuestStatus.ASSIGNED);
        assertThat(first.changeRemaining()).isEqualTo(1);

        TodayQuestResponse second = act(QuestActionType.CHANGE);
        assertThat(second.title()).isEqualTo("현관문 열고 1분 서 있기");
        assertThat(second.changeRemaining()).isZero();

        assertThatThrownBy(() -> act(QuestActionType.CHANGE))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.QUEST_CHANGE_LIMIT);
        then(missionRepository).should(times(2)).findActiveByRegionIds(anyCollection());
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
