package com.nagabolkka.backend.global.weather;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class KmaClientTest {

    @ParameterizedTest
    @CsvSource({
            "2026-11-14T10:39, 2026-11-14T09:00", // 40분 전 → 1시간 전 자료
            "2026-11-14T10:40, 2026-11-14T10:00",
            "2026-11-14T10:59, 2026-11-14T10:00",
            "2026-11-14T00:10, 2026-11-13T23:00"  // 자정 → 전날
    })
    void 기준_시각(LocalDateTime now, LocalDateTime base) {
        assertThat(KmaClient.baseTime(now)).isEqualTo(base);
    }

    @Test
    void 키가_없으면_호출_안_하고_empty() {
        // 연결 안 되는 주소 → 호출했으면 예외 · 시간 걸림
        KmaClient client = new KmaClient(new KmaProperties("", "http://localhost:1", Duration.ofSeconds(1)));

        assertThat(client.currentWeather(61, 127, LocalDateTime.of(2026, 11, 14, 10, 50))).isEmpty();
    }
}