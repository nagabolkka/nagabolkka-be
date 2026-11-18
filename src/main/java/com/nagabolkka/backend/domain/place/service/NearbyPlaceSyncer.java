package com.nagabolkka.backend.domain.place.service;

import com.nagabolkka.backend.domain.place.entity.PlaceCategory;
import com.nagabolkka.backend.global.geo.GeoPoint;
import com.nagabolkka.backend.global.kakao.KakaoLocalClient;
import com.nagabolkka.backend.global.kakao.KakaoSearchPlace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// 내 주변 실제 장소 채우기 — 처음 가는 칸만 카카오 검색 (트랜잭션 밖)
@Slf4j
@Component
@RequiredArgsConstructor
public class NearbyPlaceSyncer {

    static final int SEARCH_RADIUS_M = 3_000;
    static final Duration CELL_TTL = Duration.ofDays(7);
    static final int MAX_CELLS = 2_000; // 서버 메모리 보호 (넘으면 비우고 다시)

    enum Field { NAME, CATEGORY }

    // 검색어 → 결과 중 이 카테고리로 저장할 것 (공원 주차장 · 도서관 앞 카페 같은 것 빼기)
    record Rule(PlaceCategory category, String query, Field field, String word) {
        boolean matches(KakaoSearchPlace place) {
            String target = field == Field.NAME ? place.name() : place.categoryName();
            return target != null && target.contains(word);
        }
    }

    static final List<Rule> RULES = List.of(
            new Rule(PlaceCategory.PARK, "공원", Field.CATEGORY, "공원"),
            new Rule(PlaceCategory.LIBRARY, "도서관", Field.CATEGORY, "도서관"),
            new Rule(PlaceCategory.COMMUNITY, "행정복지센터", Field.CATEGORY, "행정"),
            new Rule(PlaceCategory.COMMUNITY, "평생학습관", Field.NAME, "평생학습"),
            new Rule(PlaceCategory.YOUTH_CENTER, "청년센터", Field.NAME, "청년"),
            new Rule(PlaceCategory.YOUTH_CENTER, "청년공간", Field.NAME, "청년")
    );

    private final KakaoLocalClient kakaoLocalClient;
    private final PlaceUpserter placeUpserter;
    private final Clock clock;

    // 칸 → 마지막으로 다 검색한 시각 (서버 메모리 · 재시작하면 다시 검색)
    private final Map<String, Instant> syncedAt = new ConcurrentHashMap<>();

    public void syncAround(GeoPoint origin) {
        String cell = cellOf(origin);
        Instant now = clock.instant();
        Instant last = syncedAt.get(cell);
        if (last != null && last.plus(CELL_TTL).isAfter(now)) {
            return;
        }

        boolean complete = true;
        int created = 0;
        for (Rule rule : RULES) {
            Optional<List<KakaoSearchPlace>> found = kakaoLocalClient.searchAll(rule.query(), origin, SEARCH_RADIUS_M);
            if (found.isEmpty()) {
                complete = false; // 키 없음 · 실패 → 다음 요청 때 다시
                continue;
            }
            List<KakaoSearchPlace> matched = found.get().stream().filter(rule::matches).toList();
            if (matched.isEmpty()) {
                continue;
            }
            try {
                created += placeUpserter.upsert(rule.category(), matched);
            } catch (DataIntegrityViolationException e) {
                complete = false; // 같은 칸 동시 요청 → 다른 쪽이 먼저 저장 (다음에 다시)
            }
        }

        if (complete) {
            if (syncedAt.size() >= MAX_CELLS) {
                syncedAt.clear();
            }
            syncedAt.put(cell, now);
        }
        if (created > 0) {
            log.info("주변 장소 {}곳 추가", created); // 좌표 · 칸 이름은 안 남김 (위치정보)
        }
    }

    // 위도 · 경도 소수 2자리 칸 (약 1.1km × 0.9km)
    static String cellOf(GeoPoint point) {
        return (long) Math.floor(point.lat() * 100) + ":" + (long) Math.floor(point.lng() * 100);
    }
}