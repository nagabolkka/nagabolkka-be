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

// 동네 지도 장소 (직접 태깅한 시드 + 카카오 로컬 주변 검색)
@Getter
@Entity
@Table(name = "places", indexes = {
        @Index(name = "idx_places_district_category", columnList = "district_id, category"),
        @Index(name = "idx_places_lat_lng", columnList = "lat, lng") // 내 위치 반경 조회
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place extends BaseEntity {

    static final int NAME_MAX = 60;
    static final int ADDRESS_MAX = 120;
    static final String KAKAO_SOURCE = "카카오 로컬";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private PlaceCategory category;

    @Column(nullable = false, length = NAME_MAX)
    private String name;

    // 시드 장소만 (카카오 장소는 구를 모름 → null)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id")
    private District district;

    @Column(nullable = false, length = ADDRESS_MAX)
    private String address;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal lat;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal lng;

    @Column(name = "is_free", nullable = false)
    private boolean free;

    // 혼자 가도 괜찮은 곳 (시드 = 직접 태깅 · 카카오 = 공공 장소라 true)
    @Column(nullable = false)
    private boolean soloOk;

    // 매일 09:00-22:00 · 월 휴관 (카카오 장소는 null → FE 「방문 전 확인해 주세요」)
    @Column(length = 60)
    private String openHours;

    @Column(length = 255)
    private String description;

    // 어느 계단 미션으로 쓰이는지 (시드만)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    // 기간 (null = 상시)
    private LocalDate validFrom;

    private LocalDate validTo;

    // 출처 (공공데이터셋 이름 · 카카오맵 확인 · 카카오 로컬)
    @Column(length = 60)
    private String source;

    // 카카오 장소 id (시드는 처음엔 null → 같은 이름 검색되면 연결)
    @Column(name = "kakao_place_id", length = 20, unique = true)
    private String kakaoPlaceId;

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

    // 카카오 로컬 검색으로 처음 본 장소 : 무료 · 혼자 OK · 상시 · 카테고리 기본 설명
    public static Place fromKakao(PlaceCategory category, String kakaoPlaceId, String name, String address,
                                  BigDecimal lat, BigDecimal lng) {
        Place place = of(category, cut(name, NAME_MAX), null, cut(address, ADDRESS_MAX), lat, lng,
                true, true, null, category.defaultDescription(), null, null, null, KAKAO_SOURCE);
        place.kakaoPlaceId = kakaoPlaceId;
        return place;
    }

    // 다시 검색됐을 때 : 이름 · 주소 · 좌표만 최신으로 (카테고리 · 설명 · 태깅은 그대로)
    public void refreshFromKakao(String name, String address, BigDecimal lat, BigDecimal lng) {
        this.name = cut(name, NAME_MAX);
        this.address = cut(address, ADDRESS_MAX);
        this.lat = lat;
        this.lng = lng;
    }

    // 직접 태깅한 시드가 카카오에서 검색됨 → id 만 연결 (중복 행 방지)
    public void linkKakao(String kakaoPlaceId) {
        this.kakaoPlaceId = kakaoPlaceId;
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

    private static String cut(String value, int max) {
        if (value == null) {
            return "";
        }
        String stripped = value.strip();
        return stripped.length() <= max ? stripped : stripped.substring(0, max);
    }
}