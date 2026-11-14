package com.nagabolkka.backend.domain.quest.service;

import com.nagabolkka.backend.domain.district.entity.District;
import com.nagabolkka.backend.domain.goal.entity.Goal;
import com.nagabolkka.backend.domain.mission.entity.Mission;
import com.nagabolkka.backend.domain.piece.entity.Piece;
import com.nagabolkka.backend.domain.piece.entity.PieceAcquisition;
import com.nagabolkka.backend.domain.piece.entity.PieceType;
import com.nagabolkka.backend.domain.piece.repository.PieceAcquisitionRepository;
import com.nagabolkka.backend.domain.piece.repository.PieceRepository;
import com.nagabolkka.backend.domain.quest.dto.CompleteQuestResponse.NextRegion;
import com.nagabolkka.backend.domain.quest.dto.CompleteQuestResponse.PieceItem;
import com.nagabolkka.backend.domain.quest.dto.CompleteQuestResponse.Restoration;
import com.nagabolkka.backend.domain.quest.entity.DailyQuest;
import com.nagabolkka.backend.domain.quest.entity.QuestRecord;
import com.nagabolkka.backend.domain.quest.entity.QuestStatus;
import com.nagabolkka.backend.domain.quest.repository.QuestRecordRepository;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.domain.region.entity.RegionStatus;
import com.nagabolkka.backend.domain.region.entity.UserRegion;
import com.nagabolkka.backend.domain.region.repository.UserRegionRepository;
import com.nagabolkka.backend.domain.region.service.NextRegionPolicy;
import com.nagabolkka.backend.domain.safety.entity.CrisisEvent;
import com.nagabolkka.backend.domain.safety.repository.CrisisEventRepository;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.global.common.Difficulty;
import com.nagabolkka.backend.global.config.ClockConfig;
import com.nagabolkka.backend.global.crypto.AesUtil;
import com.nagabolkka.backend.global.crypto.CryptoProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class QuestCompleterTest {

    private static final Long USER_ID = 1L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 14, 15, 0);
    private static final LocalDate TODAY = NOW.toLocalDate();
    private static final Clock CLOCK = Clock.fixed(
            ZonedDateTime.of(NOW, ClockConfig.KST).toInstant(), ClockConfig.KST);
    // application.yaml 의 로컬 기본 AES 키
    private static final String DEV_AES_KEY = "bG9jYWwtZGV2LW9ubHktYWVzLWtleS0zMmJ5dGVzISE=";

    @Mock QuestService questService;
    @Mock QuestRecordRepository questRecordRepository;
    @Mock CrisisEventRepository crisisEventRepository;
    @Mock PieceRepository pieceRepository;
    @Mock PieceAcquisitionRepository pieceAcquisitionRepository;
    @Mock UserRegionRepository userRegionRepository;

    QuestCompleter completer;

    User user;
    Goal goal;
    District district;
    Region stage1;
    Region stage2;
    Piece sunPiece;   // 1계단 1칸 (미션 보상)
    Piece restPiece;  // 1계단 5칸 쉼의 조각
    DailyQuest quest;
    List<UserRegion> userRegions;

    @BeforeEach
    void setUp() {
        completer = new QuestCompleter(questService, questRecordRepository, crisisEventRepository,
                pieceRepository, pieceAcquisitionRepository, userRegionRepository,
                new NextRegionPolicy(), new AesUtil(new CryptoProperties(null, DEV_AES_KEY)), CLOCK);

        stage1 = Region.of(1, "시작의 방", "집 안", "빛의 조각", "#F5D88E", "a.png", "b.png");
        ReflectionTestUtils.setField(stage1, "id", 1L);
        stage2 = Region.of(2, "바람의 숲", "문 앞", "바람의 조각", "#9DD49A", "a.png", "b.png");
        ReflectionTestUtils.setField(stage2, "id", 2L);

        sunPiece = Piece.of(stage1, 1, "햇살 조각", PieceType.EASY, "하늘에 색이 돌아왔어", "힌트");
        ReflectionTestUtils.setField(sunPiece, "id", 1L);
        restPiece = Piece.of(stage1, 5, "쉼의 조각", PieceType.REFLECTION, "방 안에 따뜻한 불이 켜졌어", "힌트");
        ReflectionTestUtils.setField(restPiece, "id", 5L);

        Mission mission = Mission.of(stage1, sunPiece, "창문 열고 바깥 공기 3분 마시기", "설명",
                Difficulty.EASY, true, false, "내 방 창가", 3);

        // 1계단 출발 : 1 ACTIVE · 2 LOCKED
        goal = Goal.of("JOB", "취업");
        district = District.of("성북구", new BigDecimal("37.5894"), new BigDecimal("127.0167"), 61, 127);
        user = User.createKakaoUser("홍윤지");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        user.completeOnboarding(5, stage1, goal, district);
        userRegions = List.of(
                UserRegion.create(user, stage1, RegionStatus.ACTIVE, NOW),
                UserRegion.create(user, stage2, RegionStatus.LOCKED, null));

        quest = DailyQuest.assign(user, TODAY, mission);
    }

    private void givenTodayQuest() {
        given(questService.lockOnboardedUser(USER_ID)).willReturn(user);
        given(questService.findOrAssign(user, TODAY)).willReturn(quest);
        given(questRecordRepository.save(any(QuestRecord.class))).willAnswer(inv -> inv.getArgument(0));
        given(userRegionRepository.findByUserIdOrderByRegionStageNoAsc(USER_ID)).willReturn(userRegions);
    }

    private QuestRecord savedRecord() {
        ArgumentCaptor<QuestRecord> captor = ArgumentCaptor.forClass(QuestRecord.class);
        then(questRecordRepository).should().save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("1계단 첫 회고 → 미션 조각 + 쉼의 조각 (둘 다 NEW) · 복원도 0 → 20 · 회고 암호화 저장")
    void firstReflection_restPiece() {
        givenTodayQuest();
        given(pieceRepository.findFirstByType(PieceType.REFLECTION)).willReturn(Optional.of(restPiece));

        QuestCompleter.Result result = completer.complete(USER_ID, 4, 0, "바람이 시원했다", false);

        assertThat(result.pieces()).extracting(PieceItem::name).containsExactly("햇살 조각", "쉼의 조각");
        assertThat(result.pieces()).allMatch(PieceItem::isNew);
        assertThat(result.restoration()).isEqualTo(new Restoration(1, "시작의 방", 0, 20));
        assertThat(quest.getStatus()).isEqualTo(QuestStatus.COMPLETED);
        then(pieceAcquisitionRepository).should(times(2)).save(any(PieceAcquisition.class));
        then(crisisEventRepository).should(never()).save(any());

        QuestRecord record = savedRecord();
        assertThat(record.getMood()).isEqualTo(4);
        assertThat(record.getReflectionEnc()).isNotNull();
        assertThat(new String(record.getReflectionEnc())).doesNotContain("바람이 시원했다"); // 원문 아님
    }

    @Test
    @DisplayName("회고 없음 → 미션 조각 1개 · 쉼의 조각 조회 안 함")
    void noReflection() {
        givenTodayQuest();

        QuestCompleter.Result result = completer.complete(USER_ID, 3, 0, null, false);

        assertThat(result.pieces()).hasSize(1);
        then(pieceRepository).shouldHaveNoInteractions();
        assertThat(savedRecord().getReflectionEnc()).isNull();
    }

    @Test
    @DisplayName("위기 → 회고 저장 안 함 · 위기 기록 1개 · 조각 · 복원도는 처리 · 쉼의 조각 없음")
    void crisis() {
        givenTodayQuest();

        QuestCompleter.Result result = completer.complete(USER_ID, 2, 0, "살기 싫어", true);

        assertThat(savedRecord().getReflectionEnc()).isNull();
        then(crisisEventRepository).should().save(any(CrisisEvent.class));
        assertThat(result.pieces()).extracting(PieceItem::name).containsExactly("햇살 조각");
        assertThat(result.restoration().after()).isEqualTo(20);
    }

    @Test
    @DisplayName("이미 받은 조각 → isNew false · 쉼의 조각은 다시 안 줌")
    void alreadyFound() {
        givenTodayQuest();
        given(pieceRepository.findFirstByType(PieceType.REFLECTION)).willReturn(Optional.of(restPiece));
        given(pieceAcquisitionRepository.existsByUserIdAndPieceId(USER_ID, 1L)).willReturn(true);
        given(pieceAcquisitionRepository.existsByUserIdAndPieceId(USER_ID, 5L)).willReturn(true);

        QuestCompleter.Result result = completer.complete(USER_ID, 4, 0, "또 나갔다", false);

        assertThat(result.pieces()).hasSize(1);
        assertThat(result.pieces().get(0).isNew()).isFalse();
        then(pieceAcquisitionRepository).should(times(1)).save(any(PieceAcquisition.class));
    }

    @Test
    @DisplayName("5번째 완료 → 복원도 80 → 100 · 다음 지역 제안 (바람의 숲)")
    void fifthCompletion_suggestNext() {
        ReflectionTestUtils.setField(userRegions.get(0), "completedCount", 4);
        givenTodayQuest();

        QuestCompleter.Result result = completer.complete(USER_ID, 5, 0, null, false);

        assertThat(result.restoration()).isEqualTo(new Restoration(1, "시작의 방", 80, 100));
        assertThat(result.nextRegion()).isEqualTo(new NextRegion(2, "바람의 숲"));
    }

    @Test
    @DisplayName("지나온 계단 미션 → 그 계단 복원도만 오름 (현재 지역은 그대로)")
    void passedStageMission() {
        user.completeOnboarding(10, stage2, goal, district); // 현재 = 2계단
        userRegions = List.of(
                UserRegion.create(user, stage1, RegionStatus.PASSED, null),
                UserRegion.create(user, stage2, RegionStatus.ACTIVE, NOW));
        givenTodayQuest();

        QuestCompleter.Result result = completer.complete(USER_ID, 4, 0, null, false);

        assertThat(result.restoration().stageNo()).isEqualTo(1);
        assertThat(userRegions.get(1).getCompletedCount()).isZero();
    }

    @Test
    @DisplayName("다음 날 난이도 : 기분 5 → 한 단계 올림 (EASY → NORMAL) · 사용자에 저장")
    void nextDifficulty_up() {
        givenTodayQuest();

        QuestCompleter.Result result = completer.complete(USER_ID, 5, 0, null, false);

        assertThat(result.nextDifficulty()).isEqualTo(Difficulty.NORMAL);
        assertThat(user.getDifficulty()).isEqualTo(Difficulty.NORMAL);
    }

    @Test
    @DisplayName("다음 날 난이도 : 위기 감지 → 가장 쉬움 (CHALLENGE → EASY)")
    void nextDifficulty_crisis() {
        ReflectionTestUtils.setField(user, "difficulty", Difficulty.CHALLENGE);
        givenTodayQuest();

        QuestCompleter.Result result = completer.complete(USER_ID, 4, 0, "살기 싫어", true);

        assertThat(result.nextDifficulty()).isEqualTo(Difficulty.EASY);
        assertThat(user.getDifficulty()).isEqualTo(Difficulty.EASY);
    }
}