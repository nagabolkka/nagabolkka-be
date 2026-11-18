package com.nagabolkka.backend.domain.place.service;

import com.nagabolkka.backend.domain.place.dto.PlaceFilter;
import com.nagabolkka.backend.domain.place.dto.PlacesResponse;
import com.nagabolkka.backend.global.geo.GeoPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    @Mock PlaceReader placeReader;
    @Mock NearbyPlaceSyncer nearbyPlaceSyncer;
    @InjectMocks PlaceService placeService;

    @Test
    @DisplayName("기준 위치 → 주변 채우기 → 같은 기준으로 조회")
    void getPlaces_order() {
        GeoPoint mine = new GeoPoint(37.5634, 127.0369);
        PlacesResponse response = new PlacesResponse(List.of());
        given(placeReader.originOf(1L, mine)).willReturn(mine);
        given(placeReader.list(mine, PlaceFilter.none())).willReturn(response);

        assertThat(placeService.getPlaces(1L, PlaceFilter.none(), mine)).isSameAs(response);

        InOrder order = inOrder(placeReader, nearbyPlaceSyncer);
        order.verify(placeReader).originOf(1L, mine);
        order.verify(nearbyPlaceSyncer).syncAround(mine);
        order.verify(placeReader).list(mine, PlaceFilter.none());
    }
}