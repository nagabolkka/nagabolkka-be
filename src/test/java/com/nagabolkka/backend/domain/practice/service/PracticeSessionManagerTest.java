package com.nagabolkka.backend.domain.practice.service;

import com.nagabolkka.backend.domain.district.entity.District;
import com.nagabolkka.backend.domain.goal.entity.Goal;
import com.nagabolkka.backend.domain.mission.entity.Mission;
import com.nagabolkka.backend.domain.piece.entity.AcquireSource;
import com.nagabolkka.backend.domain.piece.entity.Piece;
import com.nagabolkka.backend.domain.piece.entity.PieceAcquisition;
import com.nagabolkka.backend.domain.piece.entity.PieceType;
import com.nagabolkka.backend.domain.piece.repository.PieceAcquisitionRepository;
import com.nagabolkka.backend.domain.practice.entity.PracticeScenario;
import com.nagabolkka.backend.domain.practice.entity.PracticeSession;
import com.nagabolkka.backend.domain.practice.entity.PracticeStatus;
import com.nagabolkka.backend.domain.practice.repository.PracticeScenarioRepository;
import com.nagabolkka.backend.domain.practice.repository.PracticeSessionRepository;
import com.nagabolkka.backend.domain.practice.service.PracticeSessionManager.Finished;
import com.nagabolkka.backend.domain.practice.service.PracticeSessionManager.Started;
import com.nagabolkka.backend.domain.practice.service.PracticeSessionManager.Turn;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.domain.safety.entity.CrisisEvent;
import com.nagabolkka.backend.domain.safety.entity.CrisisSource;
import com.nagabolkka.backend.domain.safety.repository.CrisisEventRepository;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.common.Difficulty;
import com.nagabolkka.backend.global.config.ClockConfig;
import com.nagabolkka.backend.global.exception.ErrorCode;
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
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PracticeSessionManagerTest {

    private static final Long USER_ID = 1L;
    private static final Long SESSION_ID = 55L;
    private static final Clock CLOCK = Clock.fixed(
            ZonedDateTime.of(2026, 11, 16, 14, 0, 0, 0, ClockConfig.KST).toInstant(), ClockConfig.KST);
    private static final LocalDateTime TODAY_START = LocalDateTime.of(2026, 11, 16, 0, 0);

    @Mock UserRepository userRepository;
    @Mock PracticeScenarioRepository scenarioRepository;
    @Mock PracticeSessionRepository sessionRepository;
    @Mock PieceAcquisitionRepository pieceAcquisitionRepository;
    @Mock CrisisEventRepository crisisEventRepository;

    PracticeSessionManager manager;
    User user;
    PracticeScenario scenario;

    @BeforeEach
    void setUp() {
        manager = new PracticeSessionManager(userRepository, scenarioRepository, sessionRepository,
                pieceAcquisitionRepository, crisisEventRepository, CLOCK);

        Region stage2 = Region.of(2, "바람의 숲", "문 앞", "바람의 조각", "#9DD49A", "a.png", "b.png");
        Region stage5 = Region.of(5, "새로운 세상", "사회 연결", "희망의 조각", "#BFB2F2", "a.png", "b.png");
        Piece confidence = Piece.of(stage5, 4, "자신감 조각", PieceType.PRACTICE, "하늘에 별 하나가 떠올랐어", "힌트");
        ReflectionTestUtils.setField(confidence, "id", 24L);
        Piece challenge = Piece.of(stage5, 3, "도전 조각", PieceType.CHALLENGE, "복원", "힌트");
        Mission follow = Mission.followMission(stage5, challenge, "도서관에 프로그램 문의 전화하기", "설명",
                Difficulty.CHALLENGE, true, "집 (전화)", 5);
        ReflectionTestUtils.setField(follow, "id", 77L);
        scenario = PracticeScenario.of("FIRST_CALL", "첫 전화", "설명", "도서관 안내 직원", 3, false, confidence, follow);
        ReflectionTestUtils.setField(scenario, "id", 1L);

        user = User.createKakaoUser("홍윤지");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        District seongbuk = District.of("성북구", new BigDecimal("37.5894"), new BigDecimal("127.0167"), 61, 127);
        user.completeOnboarding(5, stage2, Goal.of("JOB", "취업"), seongbuk);
    }

    private PracticeSession givenSession(User owner) {
        PracticeSession session = PracticeSession.start(owner, scenario);
        ReflectionTestUtils.setField(session, "id", SESSION_ID);
        given(sessionRepository.findByIdForUpdate(SESSION_ID)).willReturn(Optional.of(session));
        return session;
    }

    @Test
    @DisplayName("시작 → IN_PROGRESS · 0턴 저장 · 시나리오 코드 · 최대 턴")
    void start() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(scenarioRepository.findById(1L)).willReturn(Optional.of(scenario));
        given(sessionRepository.countStartedSince(USER_ID, TODAY_START)).willReturn(2L);
        given(sessionRepository.save(any(PracticeSession.class))).willAnswer(invocation -> {
            PracticeSession saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", SESSION_ID);
            return saved;
        });

        Started started = manager.start(USER_ID, 1L);

        assertThat(started.sessionId()).isEqualTo(SESSION_ID);
        assertThat(started.scenarioCode()).isEqualTo("FIRST_CALL");
        assertThat(started.maxTurns()).isEqualTo(3);
        ArgumentCaptor<PracticeSession> captor = ArgumentCaptor.forClass(PracticeSession.class);
        then(sessionRepository).should().save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PracticeStatus.IN_PROGRESS);
        assertThat(captor.getValue().getTurnCount()).isZero();
    }

    @Test
    @DisplayName("오늘 3번 시작함 → 409 PRACTICE_DAILY_LIMIT · 저장 안 함")
    void start_dailyLimit() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(scenarioRepository.findById(1L)).willReturn(Optional.of(scenario));
        given(sessionRepository.countStartedSince(USER_ID, TODAY_START)).willReturn(3L);

        assertThatThrownBy(() -> manager.start(USER_ID, 1L))
                .extracting("errorCode").isEqualTo(ErrorCode.PRACTICE_DAILY_LIMIT);
        then(sessionRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("플러스 시나리오 → 403 PRACTICE_PLUS_ONLY")
    void start_plusOnly() {
        PracticeScenario plus = PracticeScenario.of("PLUS", "플러스", "설명", "상대", 3, true, null, null);
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(scenarioRepository.findById(9L)).willReturn(Optional.of(plus));

        assertThatThrownBy(() -> manager.start(USER_ID, 9L))
                .extracting("errorCode").isEqualTo(ErrorCode.PRACTICE_PLUS_ONLY);
    }

    @Test
    @DisplayName("시작 체크 전 → 409 ONBOARDING_REQUIRED")
    void start_onboardingRequired() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(User.createKakaoUser("새 사용자")));

        assertThatThrownBy(() -> manager.start(USER_ID, 1L))
                .extracting("errorCode").isEqualTo(ErrorCode.ONBOARDING_REQUIRED);
    }

    @Test
    @DisplayName("대화 → 턴 +1 · 시나리오 코드 · 마지막 턴 아님")
    void reserveTurn() {
        PracticeSession session = givenSession(user);

        Turn turn = manager.reserveTurn(USER_ID, SESSION_ID);

        assertThat(turn.turn()).isEqualTo(1);
        assertThat(turn.scenarioCode()).isEqualTo("FIRST_CALL");
        assertThat(turn.last()).isFalse();
        assertThat(session.getTurnCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("다른 사람 세션 → 404 NOT_FOUND · 턴 그대로")
    void reserveTurn_otherUser() {
        User other = User.createKakaoUser("다른 사람");
        ReflectionTestUtils.setField(other, "id", 2L);
        PracticeSession session = givenSession(other);

        assertThatThrownBy(() -> manager.reserveTurn(USER_ID, SESSION_ID))
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(session.getTurnCount()).isZero();
    }

    @Test
    @DisplayName("위기 → STOPPED · crisis_events PRACTICE")
    void stopForCrisis() {
        PracticeSession session = givenSession(user);
        session.nextTurn();

        manager.stopForCrisis(USER_ID, SESSION_ID);

        assertThat(session.getStatus()).isEqualTo(PracticeStatus.STOPPED);
        ArgumentCaptor<CrisisEvent> captor = ArgumentCaptor.forClass(CrisisEvent.class);
        then(crisisEventRepository).should().save(captor.capture());
        assertThat(captor.getValue().getSource()).isEqualTo(CrisisSource.PRACTICE);
    }

    @Test
    @DisplayName("끝 → COMPLETED · 연습 조각 (NEW · 계단 · 지역 이름) · 실제 미션 제안 · 조각 기록에 세션 연결")
    void complete() {
        PracticeSession session = givenSession(user);
        session.nextTurn();
        given(pieceAcquisitionRepository.existsByUserIdAndPieceId(USER_ID, 24L)).willReturn(false);

        Finished finished = manager.complete(USER_ID, SESSION_ID);

        assertThat(session.getStatus()).isEqualTo(PracticeStatus.COMPLETED);
        assertThat(finished.scenarioCode()).isEqualTo("FIRST_CALL");
        assertThat(finished.piece().name()).isEqualTo("자신감 조각");
        assertThat(finished.piece().stageNo()).isEqualTo(5);
        assertThat(finished.piece().regionName()).isEqualTo("새로운 세상");
        assertThat(finished.piece().isNew()).isTrue();
        assertThat(finished.followMission().missionId()).isEqualTo(77L);

        ArgumentCaptor<PieceAcquisition> captor = ArgumentCaptor.forClass(PieceAcquisition.class);
        then(pieceAcquisitionRepository).should().save(captor.capture());
        assertThat(captor.getValue().getSource()).isEqualTo(AcquireSource.PRACTICE);
        assertThat(captor.getValue().getPracticeSession()).isSameAs(session);
    }

    @Test
    @DisplayName("이미 끝난 세션 다시 끝 → 409 · 조각 추가 지급 없음")
    void complete_twice() {
        PracticeSession session = givenSession(user);
        session.nextTurn();
        session.complete(LocalDateTime.of(2026, 11, 16, 13, 0));

        assertThatThrownBy(() -> manager.complete(USER_ID, SESSION_ID))
                .extracting("errorCode").isEqualTo(ErrorCode.PRACTICE_SESSION_CLOSED);
        then(pieceAcquisitionRepository).should(never()).save(any());
    }
}