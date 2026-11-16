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

    // 담기 미션 제목 · 단계의 장소 자리 (DailyQuest 가 장소 이름으로 바꿈)
    public static final String PLACE_TOKEN = "{place}";

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

    // 카카오 로컬 검색어 (바깥 미션에서 실제 장소를 찾을 때) · 없으면 기본 장소 이름만
    @Column(length = 50)
    private String kakaoQuery;

    // PLACE_SEED = 동네 지도 담기 미션 (자동 배정 X) · null = 일반 미션
    @Enumerated(EnumType.STRING)
    @Column(length = 12)
    private PlaceType placeType;

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

    // 동네 지도 담기 미션 : 계단마다 1개 · active = false → 자동 배정 · 바꾸기 · 지나온 계단 후보에서 빠짐
    public static Mission placeMission(Region region, Piece rewardPiece, Difficulty difficulty) {
        Mission mission = of(region, rewardPiece, PLACE_TOKEN + " 다녀오기",
                "담은 장소에 다녀와요. 입구까지만 가도 괜찮아요.",
                difficulty, false, false, "담은 장소", 60);
        mission.placeType = PlaceType.PLACE_SEED;
        mission.active = false;
        return mission;
    }

    // 빗소리 칸 미션 → 비 오는 날에만 배정
    public boolean isRainyOnly() {
        return rewardPiece.getType() == PieceType.RAINY;
    }

    public boolean isPlaceMission() {
        return placeType == PlaceType.PLACE_SEED;
    }

    public boolean hasKakaoQuery() {
        return kakaoQuery != null && !kakaoQuery.isBlank();
    }

    // 시드 : 검색어 채우기 (기존 미션 포함)
    public void changeKakaoQuery(String kakaoQuery) {
        this.kakaoQuery = kakaoQuery;
    }
}