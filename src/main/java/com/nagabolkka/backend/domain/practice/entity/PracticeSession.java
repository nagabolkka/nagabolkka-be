package com.nagabolkka.backend.domain.practice.entity;

import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.global.common.BaseEntity;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

// 연습 세션 — 대화 원문은 저장하지 않음 (턴 수 · 상태 · 피드백만). created_at = 시작 시각
@Getter
@Entity
@Table(name = "practice_sessions",
        indexes = @Index(columnList = "user_id, created_at")) // 하루 횟수 집계
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PracticeSession extends BaseEntity {

    public static final int FEEDBACK_MAX = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private PracticeScenario scenario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private PracticeStatus status;

    @Column(nullable = false)
    private int turnCount;

    // {"good":[...],"next":[...]} JSON 문자열 (원문 대화 아님)
    @Column(length = FEEDBACK_MAX)
    private String feedback;

    private LocalDateTime endedAt;

    public static PracticeSession start(User user, PracticeScenario scenario) {
        PracticeSession session = new PracticeSession();
        session.user = user;
        session.scenario = scenario;
        session.status = PracticeStatus.IN_PROGRESS;
        session.turnCount = 0;
        return session;
    }

    public boolean isOwnedBy(Long userId) {
        return user.getId().equals(userId);
    }

    // 이번 턴 번호 (서버가 셈 · 프론트 history 길이는 믿지 않음)
    public int nextTurn() {
        checkOpen();
        if (turnCount >= scenario.getMaxTurns()) {
            throw new CustomException(ErrorCode.PRACTICE_TURN_LIMIT);
        }
        return ++turnCount;
    }

    // 위기 감지 → 멈춤 (하루 횟수는 그대로 1회 사용)
    public void stop(LocalDateTime now) {
        status = PracticeStatus.STOPPED;
        endedAt = now;
    }

    // 끝내기 : 1턴 이상 진행한 대화 중 세션만
    public void complete(LocalDateTime now) {
        checkOpen();
        if (turnCount < 1) {
            throw new CustomException(ErrorCode.PRACTICE_SESSION_CLOSED);
        }
        status = PracticeStatus.COMPLETED;
        endedAt = now;
    }

    public void saveFeedback(String json) {
        this.feedback = json != null && json.length() <= FEEDBACK_MAX ? json : null;
    }

    private void checkOpen() {
        if (status != PracticeStatus.IN_PROGRESS) {
            throw new CustomException(ErrorCode.PRACTICE_SESSION_CLOSED);
        }
    }
}