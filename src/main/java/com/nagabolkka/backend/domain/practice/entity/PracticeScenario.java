package com.nagabolkka.backend.domain.practice.entity;

import com.nagabolkka.backend.domain.mission.entity.Mission;
import com.nagabolkka.backend.domain.piece.entity.Piece;
import com.nagabolkka.backend.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 연습 시나리오 (시드 3개) — 대사 · 프롬프트는 AI 서버 scenarios/{code}.yaml
@Getter
@Entity
@Table(name = "practice_scenarios")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PracticeScenario extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // AI 서버 yaml 파일 이름과 같게 (FIRST_CALL · FIRST_VISIT · INTERVIEW)
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 40)
    private String title;

    @Column(length = 100)
    private String description;

    @Column(nullable = false, length = 40)
    private String partnerRole;

    @Column(nullable = false)
    private int maxTurns;

    // 플러스 전용 → 일반 사용자 403 (지금 시드는 모두 false)
    @Column(name = "is_plus", nullable = false)
    private boolean plus;

    // 끝내면 받는 조각 (현재 계단과 무관 · 복원도 미반영)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_piece_id")
    private Piece rewardPiece;

    // 연습 후 제안하는 실제 미션
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follow_mission_id")
    private Mission followMission;

    public static PracticeScenario of(String code, String title, String description, String partnerRole,
                                      int maxTurns, boolean plus, Piece rewardPiece, Mission followMission) {
        PracticeScenario scenario = new PracticeScenario();
        scenario.code = code;
        scenario.title = title;
        scenario.description = description;
        scenario.partnerRole = partnerRole;
        scenario.maxTurns = maxTurns;
        scenario.plus = plus;
        scenario.rewardPiece = rewardPiece;
        scenario.followMission = followMission;
        return scenario;
    }
}