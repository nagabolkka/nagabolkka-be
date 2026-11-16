package com.nagabolkka.backend.domain.place.entity;

import com.nagabolkka.backend.domain.district.entity.District;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.global.common.BaseEntity;
import com.nagabolkka.backend.global.geo.GeoPoint;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

// 동네 지도 장소 (공공 · 직접 태깅)
@Getter
@Entity
@Table(name = "places", indexes = @Index(name = "idx_places_district_category", columnList = "district_id, category"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private PlaceCategory category;

    @Column(nullable = false, length = 60)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;

    @Column(nullable = false, length = 120)
    private String address;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal lat;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal lng;

    @Column(name = "is_free", nullable = false)
    private boolean free;

    // 혼자 가도 괜찮은 곳 (직접 태깅)
    @Column(nullable = false)
    private boolean soloOk;

    // 매일 09:00-22:00 · 월 휴관
    @Column(length = 60)
    private String openHours;

    @Column(length = 255)
    private String description;

    // 어느 계단 미션으로 쓰이는지 (담기 보상 칸 · API 10)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    // 프로그램 기간 (null = 상시)
    private LocalDate validFrom;

    private LocalDate validTo;

    // 출처 (공공데이터셋 이름 · 카카오맵 확인 · 예시)
    @Column(length = 60)
    private String source;

    @Column(nullable = false)
    private boolean active;

    public static Place of(PlaceCategory category, String name, District district, String address,
                           BigDecimal lat, BigDecimal lng, boolean free, boolean soloOk,
                           String openHours, String description, Region region,
                           LocalDate validFrom, LocalDate validTo, String source) {
        Place place = new Place();
        place.category = category;
        place.name = name;
        place.district = district;
        place.address = address;
        place.lat = lat;
        place.lng = lng;
        place.free = free;
        place.soloOk = soloOk;
        place.openHours = openHours;
        place.description = description;
        place.region = region;
        place.validFrom = validFrom;
        place.validTo = validTo;
        place.source = source;
        place.active = true;
        return place;
    }

    // 그날 지도에 보이는지 : 활성 + 기간 안 (기간 null = 상시)
    public boolean isOpenOn(LocalDate date) {
        return active
                && (validFrom == null || !date.isBefore(validFrom))
                && (validTo == null || !date.isAfter(validTo));
    }

    public GeoPoint point() {
        return new GeoPoint(lat.doubleValue(), lng.doubleValue());
    }
}