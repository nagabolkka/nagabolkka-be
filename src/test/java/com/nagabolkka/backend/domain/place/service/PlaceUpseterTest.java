package com.nagabolkka.backend.domain.place.service;

import com.nagabolkka.backend.domain.place.entity.Place;
import com.nagabolkka.backend.domain.place.entity.PlaceCategory;
import com.nagabolkka.backend.domain.place.repository.PlaceRepository;
import com.nagabolkka.backend.global.kakao.KakaoSearchPlace;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PlaceUpserterTest {

    @Mock PlaceRepository placeRepository;
    @InjectMocks PlaceUpserter upserter;

    private static final KakaoSearchPlace PARK = new KakaoSearchPlace("777", "응봉근린공원", "여행 > 공원",
            "서울 성동구 응봉동 1", new BigDecimal("37.5530000"), new BigDecimal("127.0320000"));

    private static Place seed(String name) {
        return Place.of(PlaceCategory.LIBRARY, name, null, "시드 주소", new BigDecimal("37.5"), new BigDecimal("127.0"),
                true, true, "화~일 09:00-22:00", "직접 쓴 설명", null, null, null, "카카오맵 확인");
    }

    @Test
    @DisplayName("처음 본 장소 → 카카오 기본값으로 저장 (무료 · 혼자 OK · 카테고리 설명)")
    void upsert_new() {
        given(placeRepository.findByKakaoPlaceId("777")).willReturn(Optional.empty());
        given(placeRepository.findFirstByNameAndKakaoPlaceIdIsNull("응봉근린공원")).willReturn(Optional.empty());

        int created = upserter.upsert(PlaceCategory.PARK, List.of(PARK));

        assertThat(created).isEqualTo(1);
        ArgumentCaptor<Place> captor = ArgumentCaptor.forClass(Place.class);
        then(placeRepository).should().save(captor.capture());
        Place saved = captor.getValue();
        assertThat(saved.getKakaoPlaceId()).isEqualTo("777");
        assertThat(saved.getCategory()).isEqualTo(PlaceCategory.PARK);
        assertThat(saved.getDistrict()).isNull();
        assertThat(saved.isFree()).isTrue();
        assertThat(saved.isSoloOk()).isTrue();
        assertThat(saved.getDescription()).isEqualTo(PlaceCategory.PARK.defaultDescription());
        assertThat(saved.getSource()).isEqualTo("카카오 로컬");
    }

    @Test
    @DisplayName("이미 있는 카카오 장소 → 이름 · 주소 · 좌표만 최신으로 (새로 저장 X · 카테고리 그대로)")
    void upsert_known_refresh() {
        Place known = Place.fromKakao(PlaceCategory.COMMUNITY, "777", "옛 이름", "옛 주소",
                new BigDecimal("37.5"), new BigDecimal("127.0"));
        given(placeRepository.findByKakaoPlaceId("777")).willReturn(Optional.of(known));

        int created = upserter.upsert(PlaceCategory.PARK, List.of(PARK));

        assertThat(created).isZero();
        assertThat(known.getName()).isEqualTo("응봉근린공원");
        assertThat(known.getAddress()).isEqualTo("서울 성동구 응봉동 1");
        assertThat(known.getCategory()).isEqualTo(PlaceCategory.COMMUNITY);
        then(placeRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("같은 이름 시드 → 카카오 id 만 연결 (시드 태깅 · 설명 유지)")
    void upsert_linkSeed() {
        Place seed = seed("응봉근린공원");
        given(placeRepository.findByKakaoPlaceId("777")).willReturn(Optional.empty());
        given(placeRepository.findFirstByNameAndKakaoPlaceIdIsNull("응봉근린공원")).willReturn(Optional.of(seed));

        int created = upserter.upsert(PlaceCategory.PARK, List.of(PARK));

        assertThat(created).isZero();
        assertThat(seed.getKakaoPlaceId()).isEqualTo("777");
        assertThat(seed.getDescription()).isEqualTo("직접 쓴 설명");
        assertThat(seed.getOpenHours()).isEqualTo("화~일 09:00-22:00");
        then(placeRepository).should(never()).save(any());
    }
}