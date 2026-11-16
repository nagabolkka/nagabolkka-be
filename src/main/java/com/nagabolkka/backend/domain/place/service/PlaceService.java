package com.nagabolkka.backend.domain.place.service;

import com.nagabolkka.backend.domain.district.entity.District;
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
public class PlaceService {

    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;
    private final Clock clock;

    // 내 구 장소 : 오늘 열려 있는 곳 · 필터 · 거리순 (location null → 내 구 중심)
    public PlacesResponse getPlaces(Long userId, PlaceFilter filter, GeoPoint location) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        District district = user.getDistrict();
        if (!user.isOnboardingCompleted() || district == null) {
            throw new CustomException(ErrorCode.ONBOARDING_REQUIRED);
        }

        GeoPoint origin = location != null ? location : GeoPoint.of(district);
        LocalDate today = LocalDate.now(clock);

        List<PlaceItem> places = placeRepository.findActiveByDistrictId(district.getId()).stream()
                .filter(place -> place.isOpenOn(today))
                .filter(filter::matches)
                .map(place -> PlaceItem.of(place, origin.distanceTo(place.point())))
                .filter(item -> filter.withinDistance(item.distanceM()))
                .sorted(Comparator.comparingInt(PlaceItem::distanceM))
                .toList();

        return new PlacesResponse(places);
    }
}