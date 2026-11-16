package com.nagabolkka.backend.global.kakao;

import com.nagabolkka.backend.global.geo.GeoPoint;

import java.math.BigDecimal;

// 카카오 로컬 검색 결과 1개 (필요한 것만)
public record KakaoPlace(String id, String name, BigDecimal lat, BigDecimal lng) {

    public GeoPoint point() {
        return new GeoPoint(lat.doubleValue(), lng.doubleValue());
    }
}