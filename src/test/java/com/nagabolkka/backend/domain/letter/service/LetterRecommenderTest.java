package com.nagabolkka.backend.domain.letter.service;

import com.nagabolkka.backend.domain.district.entity.District;
import com.nagabolkka.backend.domain.place.entity.Place;
import com.nagabolkka.backend.domain.place.entity.PlaceCategory;
import com.nagabolkka.backend.domain.place.repository.PlaceRepository;
import com.nagabolkka.backend.global.config.ClockConfig;
import com.nagabolkka.backend.global.geo.GeoPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LetterRecommenderTest {

    private static final Clock CLOCK = Clock.fixed(
            ZonedDateTime.of(2026, 11, 24, 10, 0, 0, 0, ClockConfig.KST).toInstant(), ClockConfig.KST);
    private static final GeoPoint HERE = new GeoPoint(37.5894, 127.0167);
    private static final LocalDate WEEK = LocalDate.of(2026, 11, 16);

    @Mock PlaceRepository placeRepository;

    LetterRecommender recommender;
    District district;

    @BeforeEach
    void setUp() {
        recommender = new LetterRecommender(placeRepository, CLOCK);
        district = District.of("성북구", new BigDecimal("37.5894"), new BigDecimal("127.0167"), 61, 127);
    }

    // lat 이 클수록 HERE 에서 멂
    private Place place(Long id, double lat, boolean soloOk, LocalDate validTo) {
        Place place = Place.of(PlaceCategory.LIBRARY, "장소" + id, district, "주소", BigDecimal.valueOf(lat),
                new BigDecimal("127.0167"), true, soloOk, null, null, null, null, validTo, "test"); // free = true · soloOk
        ReflectionTestUtils.setField(place, "id", id);
        return place;
    }

    @Test
    @DisplayName("혼자 못 가는 곳 · 기간 끝난 곳은 빼고 가까운 순 2곳")
    void filterAndSort() {
        given(placeRepository.findActiveNear(HERE, 3_000)).willReturn(List.of(
                place(1L, 37.6000, true, null),
                place(2L, 37.5900, true, null),
                place(3L, 37.5895, false, null),                        // 혼자 X
                place(4L, 37.5896, true, LocalDate.of(2026, 11, 1)))); // 기간 끝

        List<Place> picked = recommender.pick(HERE, LocalDate.of(1970, 1, 5)); // offset 0 (epochDay 4 / 7 = 0)

        assertThat(picked).extracting(Place::getId).containsExactly(2L, 1L);
    }

    @Test
    @DisplayName("같은 주는 같은 결과 · 근처에 없으면 빈 목록")
    void sameWeekSameResult() {
        given(placeRepository.findActiveNear(HERE, 3_000)).willReturn(List.of(
                place(1L, 37.5900, true, null), place(2L, 37.5910, true, null), place(3L, 37.5920, true, null)));

        assertThat(recommender.pick(HERE, WEEK)).isEqualTo(recommender.pick(HERE, WEEK)).hasSize(2);

        given(placeRepository.findActiveNear(HERE, 3_000)).willReturn(List.of());
        assertThat(recommender.pick(HERE, WEEK)).isEmpty();
    }
}