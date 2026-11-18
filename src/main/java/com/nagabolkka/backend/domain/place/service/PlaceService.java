package com.nagabolkka.backend.domain.place.service;

import com.nagabolkka.backend.domain.place.dto.PlaceFilter;
import com.nagabolkka.backend.domain.place.dto.PlacesResponse;
import com.nagabolkka.backend.global.geo.GeoPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// 트랜잭션 없음 : 카카오 검색 동안 DB 연결을 잡지 않도록 (조회는 PlaceReader)
@Service
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceReader placeReader;
    private final NearbyPlaceSyncer nearbyPlaceSyncer;

    // 내 주변 장소 : 기준 위치 → (처음 가는 칸이면) 카카오로 채우기 → 반경 3km 조회
    public PlacesResponse getPlaces(Long userId, PlaceFilter filter, GeoPoint location) {
        GeoPoint origin = placeReader.originOf(userId, location);
        nearbyPlaceSyncer.syncAround(origin);
        return placeReader.list(origin, filter);
    }
}