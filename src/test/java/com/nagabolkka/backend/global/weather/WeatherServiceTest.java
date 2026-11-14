package com.nagabolkka.backend.global.weather;

import com.nagabolkka.backend.domain.district.entity.District;
import com.nagabolkka.backend.global.config.ClockConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class WeatherServiceTest {

    private static final Weather RAIN = new Weather(Weather.RAIN, true);
    private static final District SEONGBUK =
            District.of("성북구", new BigDecimal("37.5894"), new BigDecimal("127.0167"), 61, 127);

    // 시간을 앞으로 돌릴 수 있는 시계
    static class MovingClock extends Clock {
        Instant now = ZonedDateTime.of(LocalDateTime.of(2026, 11, 14, 10, 50), ClockConfig.KST).toInstant();

        void plus(Duration d) { now = now.plus(d); }
        @Override public ZoneId getZone() { return ClockConfig.KST; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private KmaClient kmaClient;
    private MovingClock clock;
    private WeatherService service;

    @BeforeEach
    void setUp() {
        kmaClient = mock(KmaClient.class);
        clock = new MovingClock();
        service = new WeatherService(kmaClient, clock);
    }

    @Test
    void 한_시간_안에는_다시_안_부름() {
        given(kmaClient.currentWeather(anyInt(), anyInt(), any())).willReturn(Optional.of(RAIN));

        assertThat(service.get(SEONGBUK)).isEqualTo(RAIN);
        clock.plus(Duration.ofMinutes(59));
        assertThat(service.get(SEONGBUK)).isEqualTo(RAIN);

        verify(kmaClient, times(1)).currentWeather(61, 127, LocalDateTime.of(2026, 11, 14, 10, 50));
    }

    @Test
    void 한_시간_지나면_다시_부름() {
        given(kmaClient.currentWeather(anyInt(), anyInt(), any()))
                .willReturn(Optional.of(RAIN), Optional.of(Weather.clear()));

        service.get(SEONGBUK);
        clock.plus(Duration.ofMinutes(61));

        assertThat(service.get(SEONGBUK)).isEqualTo(Weather.clear());
        verify(kmaClient, times(2)).currentWeather(anyInt(), anyInt(), any());
    }

    @Test
    void 실패하면_이전_값() {
        given(kmaClient.currentWeather(anyInt(), anyInt(), any()))
                .willReturn(Optional.of(RAIN), Optional.empty());

        service.get(SEONGBUK);
        clock.plus(Duration.ofMinutes(61));

        assertThat(service.get(SEONGBUK)).isEqualTo(RAIN);
    }

    @Test
    void 처음부터_실패하면_맑음_10분은_다시_안_부름() {
        given(kmaClient.currentWeather(anyInt(), anyInt(), any())).willReturn(Optional.empty());

        assertThat(service.get(SEONGBUK)).isEqualTo(Weather.clear());
        clock.plus(Duration.ofMinutes(9));
        service.get(SEONGBUK);
        verify(kmaClient, times(1)).currentWeather(anyInt(), anyInt(), any());

        clock.plus(Duration.ofMinutes(2)); // 11분
        service.get(SEONGBUK);
        verify(kmaClient, times(2)).currentWeather(anyInt(), anyInt(), any());
    }

    @Test
    void 구가_없으면_맑음_호출_안_함() {
        assertThat(service.get(null)).isEqualTo(Weather.clear());
        assertThat(service.isRainy(null)).isFalse();
        verifyNoInteractions(kmaClient);
    }
}