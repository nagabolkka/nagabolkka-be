package com.nagabolkka.backend.global.weather;

import com.nagabolkka.backend.global.geo.GeoPoint;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KmaGridTest {

    @Test
    void 기상청_격자표와_같은_값() {
        assertThat(KmaGrid.of(new GeoPoint(37.5665, 126.9780))).isEqualTo(new KmaGrid(60, 127)); // 서울시청
        assertThat(KmaGrid.of(new GeoPoint(37.5006, 127.0366))).isEqualTo(new KmaGrid(61, 125)); // 역삼역
        assertThat(KmaGrid.of(new GeoPoint(35.1796, 129.0756))).isEqualTo(new KmaGrid(98, 76));  // 부산시청
    }
}