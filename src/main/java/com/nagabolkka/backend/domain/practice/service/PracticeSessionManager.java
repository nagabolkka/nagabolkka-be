package com.nagabolkka.backend.domain.practice.service;

import com.nagabolkka.backend.domain.mission.entity.Mission;
import com.nagabolkka.backend.domain.piece.entity.Piece;
import com.nagabolkka.backend.domain.piece.entity.PieceAcquisition;
import com.nagabolkka.backend.domain.piece.repository.PieceAcquisitionRepository;
import com.nagabolkka.backend.domain.practice.dto.FinishResponse.FollowMission;
import com.nagabolkka.backend.domain.practice.dto.FinishResponse.PieceItem;
import com.nagabolkka.backend.domain.practice.entity.PracticeScenario;
import com.nagabolkka.backend.domain.practice.entity.PracticeSession;
import com.nagabolkka.backend.domain.practice.repository.PracticeScenarioRepository;
import com.nagabolkka.backend.domain.practice.repository.PracticeSessionRepository;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.domain.safety.entity.CrisisEvent;
import com.nagabolkka.backend.domain.safety.entity.CrisisSource;
import com.nagabolkka.backend.domain.safety.repository.CrisisEventRepository;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class PracticeSessionManager {

    // 하루 연습 횟수 (KST 00:00 초기화 · 시작 기준)
    public static final int DAILY_LIMIT = 3;

    private final UserRepository userRepository;
    private final PracticeScenarioRepository scenarioRepository;
    private final PracticeSessionRepository sessionRepository;
    private final PieceAcquisitionRepository pieceAcquisitionRepository;
    private final CrisisEventRepository crisisEventRepository;
    private final Clock clock;

    public record Started(Long sessionId, String scenarioCode, int maxTurns) {
    }

    public record Turn(String scenarioCode, int turn, int maxTurns) {
        public boolean last() {
            return turn >= maxTurns;
        }
    }

    public record Finished(String scenarioCode, PieceItem piece, FollowMission followMission) {
    }

    @Transactional(readOnly = true)
    public int remainingToday(Long userId) {
        long used = sessionRepository.countStartedSince(userId, todayStart());
        return (int) Math.max(0, DAILY_LIMIT - used);
    }

    // 시작 : 두 번 눌러도 횟수가 겹치지 않도록 사용자 행 잠금
    @Transactional
    public Started start(Long userId, Long scenarioId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        if (!user.isOnboardingCompleted()) {
            throw new CustomException(ErrorCode.ONBOARDING_REQUIRED);
        }
        PracticeScenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        if (scenario.isPlus()) {
            throw new CustomException(ErrorCode.PRACTICE_PLUS_ONLY);
        }
        if (sessionRepository.countStartedSince(userId, todayStart()) >= DAILY_LIMIT) {
            throw new CustomException(ErrorCode.PRACTICE_DAILY_LIMIT);
        }
        PracticeSession session = sessionRepository.save(PracticeSession.start(user, scenario));
        return new Started(session.getId(), scenario.getCode(), scenario.getMaxTurns());
    }

    // 대화 : AI 부르기 전에 턴 +1 저장 (연타 · AI 실패에도 턴 수는 서버 기준)
    @Transactional
    public Turn reserveTurn(Long userId, Long sessionId) {
        PracticeSession session = lockOwned(userId, sessionId);
        int turn = session.nextTurn(); // 끝난 세션 409 CLOSED · 턴 초과 409 TURN_LIMIT
        PracticeScenario scenario = session.getScenario();
        return new Turn(scenario.getCode(), turn, scenario.getMaxTurns());
    }

    // 위기 : 세션 멈춤 + 위기 기록 (원문 없이 시각 · 경로만)
    @Transactional
    public void stopForCrisis(Long userId, Long sessionId) {
        PracticeSession session = lockOwned(userId, sessionId);
        session.stop(LocalDateTime.now(clock));
        crisisEventRepository.save(CrisisEvent.of(session.getUser(), CrisisSource.PRACTICE));
    }

    // 끝 : COMPLETED + 연습 조각 먼저 (연타 두 번째는 409 → AI · 조각 중복 없음)
    @Transactional
    public Finished complete(Long userId, Long sessionId) {
        PracticeSession session = lockOwned(userId, sessionId);
        session.complete(LocalDateTime.now(clock)); // 0턴 · 끝난 세션 → 409 CLOSED
        PracticeScenario scenario = session.getScenario();

        PieceItem piece = grantPiece(session, scenario.getRewardPiece());
        Mission follow = scenario.getFollowMission();
        return new Finished(scenario.getCode(), piece,
                follow == null ? null : new FollowMission(follow.getId(), follow.getTitle()));
    }

    @Transactional
    public void saveFeedback(Long sessionId, String json) {
        sessionRepository.findById(sessionId).ifPresent(session -> session.saveFeedback(json));
    }

    // 현재 계단과 무관 · 복원도 미반영 · 끝낼 때마다 지급 (NEW 는 처음만)
    private PieceItem grantPiece(PracticeSession session, Piece piece) {
        if (piece == null) {
            return null;
        }
        User user = session.getUser();
        boolean isNew = !pieceAcquisitionRepository.existsByUserIdAndPieceId(user.getId(), piece.getId());
        pieceAcquisitionRepository.save(PieceAcquisition.ofPractice(user, piece, session));
        Region region = piece.getRegion();
        return new PieceItem(piece.getId(), piece.getName(), region.getStageNo(), region.getName(), isNew);
    }

    // 다른 사람 세션 · 없는 세션 → 404
    private PracticeSession lockOwned(Long userId, Long sessionId) {
        return sessionRepository.findByIdForUpdate(sessionId)
                .filter(session -> session.isOwnedBy(userId))
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
    }

    private LocalDateTime todayStart() {
        return LocalDate.now(clock).atStartOfDay();
    }
}