package com.nagabolkka.backend.global.geo;

import com.nagabolkka.backend.domain.district.entity.District;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GeoPointTest {

    private static final GeoPoint SEONGBUK = new GeoPoint(37.5894, 127.0167);

    @Test
    void 위치가_없거나_한국_밖이면_null() {
        assertThat(GeoPoint.ofNullable(null, 127.0167)).isNull();
        assertThat(GeoPoint.ofNullable(37.5894, null)).isNull();
        assertThat(GeoPoint.ofNullable(0.0, 0.0)).isNull();          // 에뮬레이터 기본값
        assertThat(GeoPoint.ofNullable(37.5894, 127.0167)).isEqualTo(SEONGBUK);
    }

    @Test
    void 구_중심() {
        District district = District.of("성북구", new BigDecimal("37.5894"), new BigDecimal("127.0167"), 61, 127);

        assertThat(GeoPoint.of(district)).isEqualTo(SEONGBUK);
        assertThat(GeoPoint.of(null)).isNull();
    }

    @Test
    void 직선거리() {
        assertThat(SEONGBUK.distanceTo(SEONGBUK)).isZero();
        assertThat(SEONGBUK.distanceTo(new GeoPoint(37.5984, 127.0167))).isCloseTo(1001, within(5)); // 위도 0.009 ≈ 1km
        assertThat(SEONGBUK.distanceTo(new GeoPoint(37.5894, 127.0267))).isCloseTo(881, within(5));  // 경도 0.01 ≈ 881m
    }
}