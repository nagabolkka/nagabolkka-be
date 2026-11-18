package com.nagabolkka.backend.domain.place.service;

import com.nagabolkka.backend.domain.place.entity.PlaceCategory;
import com.nagabolkka.backend.global.geo.GeoPoint;
import com.nagabolkka.backend.global.kakao.KakaoLocalClient;
import com.nagabolkka.backend.global.kakao.KakaoSearchPlace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class NearbyPlaceSyncerTest {

    private static final GeoPoint SEONGDONG = new GeoPoint(37.5634, 127.0369);
    private static final Instant T0 = Instant.parse("2026-11-17T01:00:00Z");

    @Mock KakaoLocalClient kakaoLocalClient;
    @Mock PlaceUpserter placeUpserter;
    @Mock Clock clock;

    NearbyPlaceSyncer syncer;

    @BeforeEach
    void setUp() {
        syncer = new NearbyPlaceSyncer(kakaoLocalClient, placeUpserter, clock);
    }

    private static KakaoSearchPlace kakao(String id, String name, String categoryName) {
        return new KakaoSearchPlace(id, name, categoryName, "서울 성동구", new BigDecimal("37.5634"), new BigDecimal("127.0369"));
    }

    private void givenEveryQueryReturns(Optional<List<KakaoSearchPlace>> result) {
        given(kakaoLocalClient.searchAll(anyString(), any(GeoPoint.class), eq(3_000))).willReturn(result);
    }

    @Test
    @DisplayName("처음 가는 칸 → 검색어 6개 · 규칙에 맞는 것만 저장 (도서관 앞 카페 · 공원 주차장 제외)")
    void sync_filtersByRule() {
        given(clock.instant()).willReturn(T0);
        givenEveryQueryReturns(Optional.of(List.of()));
        KakaoSearchPlace library = kakao("1", "성동구립도서관", "문화,예술 > 도서관");
        KakaoSearchPlace cafe = kakao("2", "도서관옆카페", "음식점 > 카페");
        KakaoSearchPlace park = kakao("3", "응봉근린공원", "여행 > 공원 > 도시근린공원");
        KakaoSearchPlace parking = kakao("4", "응봉공원 공영주차장", "교통,수송 > 주차장");
        given(kakaoLocalClient.searchAll(eq("도서관"), any(GeoPoint.class), anyInt()))
                .willReturn(Optional.of(List.of(library, cafe)));
        given(kakaoLocalClient.searchAll(eq("공원"), any(GeoPoint.class), anyInt()))
                .willReturn(Optional.of(List.of(park, parking)));

        syncer.syncAround(SEONGDONG);

        then(kakaoLocalClient).should(times(6)).searchAll(anyString(), eq(SEONGDONG), eq(3_000));
        then(placeUpserter).should().upsert(PlaceCategory.LIBRARY, List.of(library));
        then(placeUpserter).should().upsert(PlaceCategory.PARK, List.of(park));
        then(placeUpserter).should(times(2)).upsert(any(), any());
    }

    @Test
    @DisplayName("같은 칸을 7일 안에 다시 → 카카오 안 부름")
    void sync_sameCellWithinTtl_skip() {
        given(clock.instant()).willReturn(T0, T0.plus(Duration.ofDays(6)));
        givenEveryQueryReturns(Optional.of(List.of()));

        syncer.syncAround(SEONGDONG);
        syncer.syncAround(new GeoPoint(37.5639, 127.0362)); // 같은 칸 (소수 2자리 같음)

        then(kakaoLocalClient).should(times(6)).searchAll(anyString(), any(GeoPoint.class), anyInt());
    }

    @Test
    @DisplayName("7일 지나면 다시 검색")
    void sync_afterTtl_again() {
        given(clock.instant()).willReturn(T0, T0.plus(Duration.ofDays(8)));
        givenEveryQueryReturns(Optional.of(List.of()));

        syncer.syncAround(SEONGDONG);
        syncer.syncAround(SEONGDONG);

        then(kakaoLocalClient).should(times(12)).searchAll(anyString(), any(GeoPoint.class), anyInt());
    }

    @Test
    @DisplayName("키 없음 · 실패 (empty) → 저장 안 하고 칸도 기억 안 함 → 다음 요청 때 다시")
    void sync_failure_notCached() {
        given(clock.instant()).willReturn(T0);
        givenEveryQueryReturns(Optional.empty());

        syncer.syncAround(SEONGDONG);
        syncer.syncAround(SEONGDONG);

        then(placeUpserter).should(never()).upsert(any(), any());
        then(kakaoLocalClient).should(times(12)).searchAll(anyString(), any(GeoPoint.class), anyInt());
    }

    @Test
    @DisplayName("동시 요청으로 유니크 충돌 → 나머지 검색은 계속 · 칸은 기억 안 함")
    void sync_duplicate_continueAndNotCached() {
        given(clock.instant()).willReturn(T0);
        givenEveryQueryReturns(Optional.of(List.of()));
        given(kakaoLocalClient.searchAll(eq("도서관"), any(GeoPoint.class), anyInt()))
                .willReturn(Optional.of(List.of(kakao("1", "성동구립도서관", "문화,예술 > 도서관"))));
        given(placeUpserter.upsert(eq(PlaceCategory.LIBRARY), any())).willThrow(new DataIntegrityViolationException("uk"));

        syncer.syncAround(SEONGDONG);
        syncer.syncAround(SEONGDONG);

        then(kakaoLocalClient).should(times(12)).searchAll(anyString(), any(GeoPoint.class), anyInt());
    }

    @Test
    @DisplayName("칸 = 위도 · 경도 소수 2자리 (내림)")
    void cellOf() {
        assertThat(NearbyPlaceSyncer.cellOf(new GeoPoint(37.5634, 127.0369))).isEqualTo("3756:12703");
        assertThat(NearbyPlaceSyncer.cellOf(new GeoPoint(37.5699, 127.0301))).isEqualTo("3756:12703");
        assertThat(NearbyPlaceSyncer.cellOf(new GeoPoint(37.5710, 127.0369))).isEqualTo("3757:12703");
    }
}