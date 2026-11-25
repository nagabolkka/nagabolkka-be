package com.nagabolkka.backend.domain.letter.service;

import com.nagabolkka.backend.domain.place.service.NearbyPlaceSyncer;
import com.nagabolkka.backend.global.geo.GeoPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class LetterServiceTest {

    @Mock LetterReader letterReader;
    @Mock NearbyPlaceSyncer nearbyPlaceSyncer;

    @InjectMocks LetterService service;

    @Test
    @DisplayName("위치 있음 → 카카오로 주변 장소 채운 뒤 조회")
    void withLocation() {
        GeoPoint here = new GeoPoint(37.5446, 127.0560);
        given(letterReader.getLatest(1L, here)).willReturn(Optional.empty());

        service.getLatest(1L, here);

        InOrder order = inOrder(nearbyPlaceSyncer, letterReader);
        order.verify(nearbyPlaceSyncer).syncAround(here);
        order.verify(letterReader).getLatest(1L, here);
    }

    @Test
    @DisplayName("위치 없음 → 카카오 검색 안 함")
    void withoutLocation() {
        given(letterReader.getLatest(1L, null)).willReturn(Optional.empty());

        service.getLatest(1L, null);

        then(nearbyPlaceSyncer).should(org.mockito.Mockito.never()).syncAround(any());
    }
}