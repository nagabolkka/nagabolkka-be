package com.nagabolkka.backend.domain.place.dto;

import com.nagabolkka.backend.domain.place.entity.Place;
import com.nagabolkka.backend.domain.place.entity.PlaceCategory;

// 장소 목록 필터 (category null = 전체 · within null = 거리 제한 없음)
public record PlaceFilter(PlaceCategory category, boolean free, boolean solo, Integer within) {

    public static PlaceFilter none() {
        return new PlaceFilter(null, false, false, null);
    }

    public boolean matches(Place place) {
        return (category == null || place.getCategory() == category)
                && (!free || place.isFree())
                && (!solo || place.isSoloOk());
    }

    public boolean withinDistance(int distanceM) {
        return within == null || distanceM <= within;
    }
}