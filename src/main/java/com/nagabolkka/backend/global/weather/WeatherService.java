package com.nagabolkka.backend.global.weather;

import com.nagabolkka.backend.domain.district.entity.District;
import com.nagabolkka.backend.global.geo.GeoPoint;
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
    static final int MAX_CACHE = 2_000; // 격자 수 상한 (서버 메모리 보호)

    private final KmaClient kmaClient;
    private final Clock clock;

    // 격자 "nx,ny" → 날씨 · 만료 시각
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    private record Cached(Weather weather, LocalDateTime expiresAt) {
    }

    // 지금 위치 격자 → 위치 없으면 내 구 격자 → 구도 없으면 맑음 (좌표는 격자 계산에만, 저장 X)
    public Weather get(District district, GeoPoint location) {
        if (location != null) {
            KmaGrid grid = KmaGrid.of(location);
            return byGrid(grid.nx(), grid.ny());
        }
        if (district == null) {
            return Weather.clear();
        }
        return byGrid(district.getNx(), district.getNy());
    }

    // 위치 없이 (기존 호출 · 테스트 그대로)
    public Weather get(District district) {
        return get(district, null);
    }

    // 배정용 (Mockito mock 이면 기본 false → 기존 테스트가 그대로 돎)
    public boolean isRainy(District district, GeoPoint location) {
        return get(district, location).isRainy();
    }

    public boolean isRainy(District district) {
        return isRainy(district, null);
    }

    private Weather byGrid(int nx, int ny) {
        String key = nx + "," + ny;
        LocalDateTime now = LocalDateTime.now(clock);

        Cached cached = cache.get(key);
        if (cached != null && now.isBefore(cached.expiresAt())) {
            return cached.weather();
        }

        Optional<Weather> fetched = kmaClient.currentWeather(nx, ny, now);
        Weather weather = fetched.orElseGet(() -> cached != null ? cached.weather() : Weather.clear());
        if (cache.size() >= MAX_CACHE) {
            cache.clear();
        }
        cache.put(key, new Cached(weather, now.plus(fetched.isPresent() ? CACHE_TTL : RETRY_AFTER_FAIL)));
        return weather;
    }
}