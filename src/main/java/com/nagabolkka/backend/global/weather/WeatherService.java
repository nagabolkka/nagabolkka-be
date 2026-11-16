package com.nagabolkka.backend.global.weather;

import com.nagabolkka.backend.domain.district.entity.District;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class WeatherService {

    static final Duration CACHE_TTL = Duration.ofHours(1);
    // 실패하면 이 시간 동안 다시 안 부름 → 기상청 장애 때 홈마다 2초씩 기다리지 않게
    static final Duration RETRY_AFTER_FAIL = Duration.ofMinutes(10);

    private final KmaClient kmaClient;
    private final Clock clock;

    // 격자 "nx,ny" → 날씨 · 만료 시각
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    private record Cached(Weather weather, LocalDateTime expiresAt) {
    }

    // 내 구 날씨 · 구가 없으면 맑음 (온보딩 전 · 데이터 오류)
    public Weather get(District district) {
        if (district == null) {
            return Weather.clear();
        }
        String key = district.getNx() + "," + district.getNy();
        LocalDateTime now = LocalDateTime.now(clock);

        Cached cached = cache.get(key);
        if (cached != null && now.isBefore(cached.expiresAt())) {
            return cached.weather();
        }

        Optional<Weather> fetched = kmaClient.currentWeather(district.getNx(), district.getNy(), now);
        Weather weather = fetched.orElseGet(() -> cached != null ? cached.weather() : Weather.clear());
        cache.put(key, new Cached(weather, now.plus(fetched.isPresent() ? CACHE_TTL : RETRY_AFTER_FAIL)));
        return weather;
    }

    // 배정용 (Mockito mock 이면 기본 false → 기존 테스트가 그대로 돎)
    public boolean isRainy(District district) {
        return get(district).isRainy();
    }
}