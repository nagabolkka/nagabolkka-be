package com.nagabolkka.backend.global.geo;

import java.math.BigDecimal;
import java.math.RoundingMode;

// 위도 · 경도 네모
public record GeoBox(BigDecimal minLat, BigDecimal maxLat, BigDecimal minLng, BigDecimal maxLng) {

    static GeoBox of(double minLat, double maxLat, double minLng, double maxLng) {
        return new GeoBox(scale(minLat), scale(maxLat), scale(minLng), scale(maxLng));
    }

    private static BigDecimal scale(double value) {
        return BigDecimal.valueOf(value).setScale(7, RoundingMode.HALF_UP); // places.lat · lng 와 같은 자릿수
    }
}