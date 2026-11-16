package com.nagabolkka.backend.domain.practice.entity;

import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PracticeSessionTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 16, 14, 0);

    User user;
    PracticeSession session;

    @BeforeEach
    void setUp() {
        user = User.createKakaoUser("홍윤지");
        ReflectionTestUtils.setField(user, "id", 1L);
        PracticeScenario scenario = PracticeScenario.of("FIRST_CALL", "첫 전화", "설명", "도서관 안내 직원",
                3, false, null, null);
        session = PracticeSession.start(user, scenario);
    }

    @Test
    @DisplayName("턴은 1 ~ maxTurns · 넘으면 409 PRACTICE_TURN_LIMIT")
    void nextTurn() {
        assertThat(session.nextTurn()).isEqualTo(1);
        assertThat(session.nextTurn()).isEqualTo(2);
        assertThat(session.nextTurn()).isEqualTo(3);

        assertThatThrownBy(session::nextTurn).extracting("errorCode").isEqualTo(ErrorCode.PRACTICE_TURN_LIMIT);
        assertThat(session.getTurnCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("위기로 멈춘 세션 → 대화 · 끝 모두 409 PRACTICE_SESSION_CLOSED")
    void stopped() {
        session.nextTurn();
        session.stop(NOW);

        assertThat(session.getStatus()).isEqualTo(PracticeStatus.STOPPED);
        assertThat(session.getEndedAt()).isEqualTo(NOW);
        assertThatThrownBy(session::nextTurn).extracting("errorCode").isEqualTo(ErrorCode.PRACTICE_SESSION_CLOSED);
        assertThatThrownBy(() -> session.complete(NOW)).extracting("errorCode").isEqualTo(ErrorCode.PRACTICE_SESSION_CLOSED);
    }

    @Test
    @DisplayName("0턴에서 끝 → 409 PRACTICE_SESSION_CLOSED")
    void complete_noTurn() {
        assertThatThrownBy(() -> session.complete(NOW)).extracting("errorCode").isEqualTo(ErrorCode.PRACTICE_SESSION_CLOSED);
        assertThat(session.getStatus()).isEqualTo(PracticeStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("1턴 이상 → COMPLETED · 두 번째 끝은 409")
    void complete_twice() {
        session.nextTurn();
        session.complete(NOW);

        assertThat(session.getStatus()).isEqualTo(PracticeStatus.COMPLETED);
        assertThatThrownBy(() -> session.complete(NOW)).extracting("errorCode").isEqualTo(ErrorCode.PRACTICE_SESSION_CLOSED);
    }

    @Test
    @DisplayName("피드백 500자 넘으면 저장 안 함")
    void saveFeedback() {
        session.saveFeedback("{\"good\":[\"a\"],\"next\":[\"b\"]}");
        assertThat(session.getFeedback()).startsWith("{\"good\"");

        session.saveFeedback("x".repeat(PracticeSession.FEEDBACK_MAX + 1));
        assertThat(session.getFeedback()).isNull();
    }

    @Test
    @DisplayName("내 세션인지")
    void isOwnedBy() {
        assertThat(session.isOwnedBy(1L)).isTrue();
        assertThat(session.isOwnedBy(2L)).isFalse();
    }
}