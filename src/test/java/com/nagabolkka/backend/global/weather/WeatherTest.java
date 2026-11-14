package com.nagabolkka.backend.global.weather;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherTest {

    @ParameterizedTest
    @CsvSource({
            "0, CLEAR, false",
            "1, RAIN, true",
            "2, RAIN, true",
            "5, RAIN, true",
            "6, RAIN, true",
            "3, SNOW, true",
            "7, SNOW, true",
            "' 1 ', RAIN, true",
            "9, CLEAR, false"    // 모르는 값 → 맑음
    })
    void PTY_로_날씨(String pty, String sky, boolean rainy) {
        Weather weather = Weather.fromPty(pty);

        assertThat(weather.sky()).isEqualTo(sky);
        assertThat(weather.isRainy()).isEqualTo(rainy);
    }

    @Test
    void PTY_없으면_맑음() {
        assertThat(Weather.fromPty(null)).isEqualTo(Weather.clear());
    }
}