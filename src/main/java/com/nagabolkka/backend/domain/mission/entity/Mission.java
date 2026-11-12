package com.nagabolkka.backend.domain.mission.entity;

import com.nagabolkka.backend.domain.piece.entity.Piece;
import com.nagabolkka.backend.domain.piece.entity.PieceType;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.global.common.BaseEntity;
import com.nagabolkka.backend.global.common.Difficulty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

// 오늘 퀘스트로 배정되는 미션. 완료하면 rewardPiece 를 받는다
@Getter
@Entity
@Table(name = "missions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reward_piece_id", nullable = false)
    private Piece rewardPiece;

    @Column(nullable = false, length = 60)
    private String title;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Difficulty difficulty;

    @Column(name = "is_indoor", nullable = false)
    private boolean indoor;

    // 비 오는 날 배정 가능 (빗소리 칸 미션 · 5계단 도전 미션)
    @Column(nullable = false)
    private boolean rainyOk;

    // 기본 장소 이름
    @Column(nullable = false, length = 60)
    private String placeName;

    @Column(nullable = false)
    private int estMinutes;

    @Column(nullable = false)
    private boolean active;

    public static Mission of(Region region, Piece rewardPiece, String title, String description,
                             Difficulty difficulty, boolean indoor, boolean rainyOk,
                             String placeName, int estMinutes) {
        Mission mission = new Mission();
        mission.region = region;
        mission.rewardPiece = rewardPiece;
        mission.title = title;
        mission.description = description;
        mission.difficulty = difficulty;
        mission.indoor = indoor;
        mission.rainyOk = rainyOk;
        mission.placeName = placeName;
        mission.estMinutes = estMinutes;
        mission.active = true;
        return mission;
    }

    // 빗소리 칸 미션 → 비 오는 날에만 배정
    public boolean isRainyOnly() {
        return rewardPiece.getType() == PieceType.RAINY;
    }
}