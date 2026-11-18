package com.nagabolkka.backend.domain.place.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceTest {

    private static Place place(LocalDate from, LocalDate to) {
        return Place.of(PlaceCategory.COMMUNITY, "북토크", null, "주소", new BigDecimal("37.5"), new BigDecimal("127.0"),
                true, true, null, null, null, from, to, "예시");
    }

    @Test
    void 기간이_없으면_항상() {
        assertThat(place(null, null).isOpenOn(LocalDate.of(2030, 1, 1))).isTrue();
    }

    @Test
    void 기간_안_첫날_마지막날_포함() {
        Place place = place(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        assertThat(place.isOpenOn(LocalDate.of(2026, 11, 1))).isTrue();
        assertThat(place.isOpenOn(LocalDate.of(2026, 11, 30))).isTrue();
        assertThat(place.isOpenOn(LocalDate.of(2026, 10, 31))).isFalse();
        assertThat(place.isOpenOn(LocalDate.of(2026, 12, 1))).isFalse();
    }

    @Test
    void 시작일만_있으면_그날부터() {
        Place place = place(LocalDate.of(2026, 11, 20), null);

        assertThat(place.isOpenOn(LocalDate.of(2026, 11, 19))).isFalse();
        assertThat(place.isOpenOn(LocalDate.of(2027, 1, 1))).isTrue();
    }
}