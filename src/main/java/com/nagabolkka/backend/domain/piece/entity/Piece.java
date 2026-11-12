package com.nagabolkka.backend.domain.piece.entity;

import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.global.common.BaseEntity;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 도감 조각 (5지역 × 5칸 = 25 + 보너스)
@Getter
@Entity
@Table(name = "pieces",
        uniqueConstraints = @UniqueConstraint(columnNames = {"region_id", "slot_no"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Piece extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // BONUS 는 null (도감 칸 밖)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    // 도감 칸 1~5 · BONUS 는 null
    private Integer slotNo;

    @Column(nullable = false, length = 20)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PieceType type;

    @Column(length = 100)
    private String restoreText;

    // 못 찾은 조각을 눌렀을 때 힌트
    @Column(length = 100)
    private String hint;

    public static Piece of(Region region, Integer slotNo, String name, PieceType type,
                           String restoreText, String hint) {
        Piece piece = new Piece();
        piece.region = region;
        piece.slotNo = slotNo;
        piece.name = name;
        piece.type = type;
        piece.restoreText = restoreText;
        piece.hint = hint;
        return piece;
    }
}