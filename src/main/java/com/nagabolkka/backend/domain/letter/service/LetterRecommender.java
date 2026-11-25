package com.nagabolkka.backend.domain.letter.service;

import com.nagabolkka.backend.domain.place.entity.Place;
import com.nagabolkka.backend.domain.place.repository.PlaceRepository;
import com.nagabolkka.backend.global.geo.GeoPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

// 편지 추천 장소 고르기 (편지를 열 때 · 지금 위치 기준)
// 규칙은 배치 (WeeklyLetterGenerator.recommend) 와 같음 : 3km · 지금 열린 · 혼자 OK → 가까운 6곳 중 주마다 돌아가며 2곳
@Component
@RequiredArgsConstructor
public class LetterRecommender {

    static final int COUNT = 2;
    static final int POOL = 6;
    static final int RADIUS_M = 3_000; // 동네 지도와 같은 반경

    private final PlaceRepository placeRepository;
    private final Clock clock;

    public List<Place> pick(GeoPoint center, LocalDate weekStart) {
        LocalDate today = LocalDate.now(clock);
        List<Place> pool = placeRepository.findActiveNear(center, RADIUS_M).stream()
                .filter(place -> place.isOpenOn(today))
                .filter(Place::isSoloOk)
                .sorted(Comparator.comparingInt((Place place) -> center.distanceTo(place.point()))
                        .thenComparing(Place::getId))
                .limit(POOL)
                .toList();
        if (pool.isEmpty()) {
            return List.of();
        }
        // 같은 주에는 같은 2곳 · 다음 주에는 다음 2곳 (새로고침해도 안 바뀜)
        int offset = (int) ((weekStart.toEpochDay() / 7 * COUNT) % pool.size());
        return IntStream.range(0, Math.min(COUNT, pool.size()))
                .mapToObj(i -> pool.get((offset + i) % pool.size()))
                .toList();
    }
}