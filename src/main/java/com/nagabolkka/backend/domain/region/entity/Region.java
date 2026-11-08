package com.nagabolkka.backend.domain.region.entity;

import com.nagabolkka.backend.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 세계관 지역 = 계단 1~5 (시작의 방 · 바람의 숲 · 발견의 마을 · 만남의 정원 · 새로운 세상)
@Getter
@Entity
@Table(name = "regions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Region extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 계단 번호 1~5
    @Column(nullable = false, unique = true)
    private int stageNo;

    // 시작의 방
    @Column(nullable = false, length = 20)
    private String name;

    // 집 안
    @Column(nullable = false, length = 20)
    private String realLabel;

    // 빛의 조각
    @Column(nullable = false, length = 20)
    private String pieceFamily;

    @Column(nullable = false, length = 7, columnDefinition = "char(7)")
    private String color;

    // 지역 배경 (맑음 · 비) — 파일은 정적 저장소, DB 엔 주소만
    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private String rainyImageUrl;

    public static Region of(int stageNo, String name, String realLabel, String pieceFamily,
                            String color, String imageUrl, String rainyImageUrl) {
        Region region = new Region();
        region.stageNo = stageNo;
        region.name = name;
        region.realLabel = realLabel;
        region.pieceFamily = pieceFamily;
        region.color = color;
        region.imageUrl = imageUrl;
        region.rainyImageUrl = rainyImageUrl;
        return region;
    }
}