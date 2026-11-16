package com.nagabolkka.backend.domain.piece.entity;

import com.nagabolkka.backend.domain.quest.entity.DailyQuest;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.global.common.BaseEntity;
import com.nagabolkka.backend.domain.goal.entity.Goal;
import com.nagabolkka.backend.domain.practice.entity.PracticeSession;
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

// 조각 획득 기록 (누적만, 감소 없음)
@Getter
@Entity
@Table(name = "piece_acquisitions",
        indexes = @Index(columnList = "user_id, piece_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PieceAcquisition extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "piece_id", nullable = false)
    private Piece piece;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AcquireSource source;

    // 퀘스트로 얻었을 때만
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_quest_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DailyQuest dailyQuest;

    // 희망 조각을 어느 목표로 받았는지 (목표마다 처음 4/4 에 1번)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id")
    private Goal goal;

    // 연습으로 얻었을 때만
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practice_session_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private PracticeSession practiceSession;

    public static PieceAcquisition of(User user, Piece piece, AcquireSource source, DailyQuest dailyQuest) {
        PieceAcquisition acquisition = new PieceAcquisition();
        acquisition.user = user;
        acquisition.piece = piece;
        acquisition.source = source;
        acquisition.dailyQuest = dailyQuest;
        return acquisition;
    }

    // 목표 4/4 완성 → 희망 조각 (source = GOAL)
    public static PieceAcquisition ofGoal(User user, Piece piece, Goal goal) {
        PieceAcquisition acquisition = of(user, piece, AcquireSource.GOAL, null);
        acquisition.goal = goal;
        return acquisition;
    }

    // 연습 끝 → 연습 조각 (source = PRACTICE)
    public static PieceAcquisition ofPractice(User user, Piece piece, PracticeSession session) {
        PieceAcquisition acquisition = of(user, piece, AcquireSource.PRACTICE, null);
        acquisition.practiceSession = session;
        return acquisition;
    }
}