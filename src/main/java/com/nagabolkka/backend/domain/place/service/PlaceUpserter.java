package com.nagabolkka.backend.domain.place.service;

import com.nagabolkka.backend.domain.place.entity.Place;
import com.nagabolkka.backend.domain.place.entity.PlaceCategory;
import com.nagabolkka.backend.domain.place.repository.PlaceRepository;
import com.nagabolkka.backend.global.kakao.KakaoSearchPlace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

// 카카오 검색 결과 → places (카카오 id 로 1번만) — 검색어 1개 = 트랜잭션 1개
@Component
@RequiredArgsConstructor
public class PlaceUpserter {

    private final PlaceRepository placeRepository;

    // 반환 = 새로 저장한 수
    @Transactional
    public int upsert(PlaceCategory category, List<KakaoSearchPlace> found) {
        int created = 0;
        for (KakaoSearchPlace kakao : found) {
            Optional<Place> known = placeRepository.findByKakaoPlaceId(kakao.id());
            if (known.isPresent()) {
                known.get().refreshFromKakao(kakao.name(), kakao.address(), kakao.lat(), kakao.lng());
                continue; // 다른 검색어로 이미 저장 → 카테고리 그대로
            }
            Optional<Place> seed = placeRepository.findFirstByNameAndKakaoPlaceIdIsNull(kakao.name());
            if (seed.isPresent()) {
                seed.get().linkKakao(kakao.id()); // 시드 (아리랑도서관 등) 는 태깅 · 설명 유지
                continue;
            }
            placeRepository.save(Place.fromKakao(category, kakao.id(), kakao.name(), kakao.address(),
                    kakao.lat(), kakao.lng()));
            created++;
        }
        return created;
    }
}