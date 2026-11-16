package com.nagabolkka.backend.global.geo;

import com.nagabolkka.backend.domain.district.entity.District;

// 좌표 (위도 · 경도) — 사용자 위치는 계산에만 쓰고 저장하지 않는다
public record GeoPoint(double lat, double lng) {

    private static final double EARTH_RADIUS_M = 6_371_000;

    // 앱이 보낸 위치 · 둘 중 하나라도 없거나 한국 밖이면 null (→ 내 구 중심)
    public static GeoPoint ofNullable(Double lat, Double lng) {
        if (lat == null || lng == null) {
            return null;
        }
        if (lat < 33 || lat > 39 || lng < 124 || lng > 132) {
            return null;
        }
        return new GeoPoint(lat, lng);
    }

    // 구 중심 · 구가 없으면 null
    public static GeoPoint of(District district) {
        if (district == null) {
            return null;
        }
        return new GeoPoint(district.getCenterLat().doubleValue(), district.getCenterLng().doubleValue());
    }

    // 직선거리 (m, 하버사인)
    public int distanceTo(GeoPoint other) {
        double dLat = Math.toRadians(other.lat - lat);
        double dLng = Math.toRadians(other.lng - lng);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(other.lat))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return (int) Math.round(EARTH_RADIUS_M * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    }
}