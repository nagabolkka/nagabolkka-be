package com.nagabolkka.backend.domain.place.service;

import com.nagabolkka.backend.domain.place.dto.PlaceFilter;
import com.nagabolkka.backend.domain.place.dto.PlacesResponse;
import com.nagabolkka.backend.domain.place.dto.PlacesResponse.PlaceItem;
import com.nagabolkka.backend.domain.place.repository.PlaceRepository;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import com.nagabolkka.backend.global.geo.GeoPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceReader {

    static final int LIST_RADIUS_M = NearbyPlaceSyncer.SEARCH_RADIUS_M; // 3km

    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;
    private final Clock clock;

    // 기준 위치 : 보낸 위치 → 없으면 내 구 중심 · 시작 체크 전 409
    public GeoPoint originOf(Long userId, GeoPoint location) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        if (!user.isOnboardingCompleted() || user.getDistrict() == null) {
            throw new CustomException(ErrorCode.ONBOARDING_REQUIRED);
        }
        return location != null ? location : GeoPoint.of(user.getDistrict());
    }

    // 반경 3km · 오늘 열려 있는 곳 · 필터 · 거리순
    public PlacesResponse list(GeoPoint origin, PlaceFilter filter) {
        LocalDate today = LocalDate.now(clock);
        List<PlaceItem> places = placeRepository.findActiveNear(origin, LIST_RADIUS_M).stream()
                .filter(place -> place.isOpenOn(today))
                .filter(filter::matches)
                .map(place -> PlaceItem.of(place, origin.distanceTo(place.point())))
                .filter(item -> item.distanceM() <= LIST_RADIUS_M) // 네모 모서리 빼기
                .filter(item -> filter.withinDistance(item.distanceM()))
                .sorted(Comparator.comparingInt(PlaceItem::distanceM))
                .toList();
        return new PlacesResponse(places);
    }
}