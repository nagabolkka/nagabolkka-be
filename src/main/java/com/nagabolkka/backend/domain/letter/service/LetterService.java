package com.nagabolkka.backend.domain.letter.service;

import com.nagabolkka.backend.domain.letter.dto.LetterResponse;
import com.nagabolkka.backend.domain.place.service.NearbyPlaceSyncer;
import com.nagabolkka.backend.global.geo.GeoPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

// 트랜잭션 없음 : 카카오 검색 (처음 가는 칸만) 동안 DB 연결을 잡지 않도록 · 조회는 LetterReader
@Service
@RequiredArgsConstructor
public class LetterService {

    private final LetterReader letterReader;
    private final NearbyPlaceSyncer nearbyPlaceSyncer;

    // location : 편지를 여는 순간의 위치 (저장 X) · 없으면 편지를 만들 때 고른 장소
    public Optional<LetterResponse> getLatest(Long userId, GeoPoint location) {
        if (location != null) {
            nearbyPlaceSyncer.syncAround(location); // 동네 지도를 안 열어 본 동네도 장소가 있게 (동네 지도와 같은 방식)
        }
        return letterReader.getLatest(userId, location);
    }
}