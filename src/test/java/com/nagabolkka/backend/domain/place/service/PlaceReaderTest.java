package com.nagabolkka.backend.domain.place.service;

import com.nagabolkka.backend.domain.district.entity.District;
import com.nagabolkka.backend.domain.goal.entity.Goal;
import com.nagabolkka.backend.domain.place.dto.PlaceFilter;
import com.nagabolkka.backend.domain.place.dto.PlacesResponse;
import com.nagabolkka.backend.domain.place.dto.PlacesResponse.PlaceItem;
import com.nagabolkka.backend.domain.place.entity.Place;
import com.nagabolkka.backend.domain.place.entity.PlaceCategory;
import com.nagabolkka.backend.domain.place.repository.PlaceRepository;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.config.ClockConfig;
import com.nagabolkka.backend.global.exception.ErrorCode;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PlaceReaderTest {

    private static final Long USER_ID = 1L;
    private static final Clock CLOCK = Clock.fixed(
            ZonedDateTime.of(2026, 11, 16, 10, 0, 0, 0, ClockConfig.KST).toInstant(), ClockConfig.KST);
    private static final GeoPoint SEONGBUK = new GeoPoint(37.5894, 127.0167);

    @Mock UserRepository userRepository;
    @Mock PlaceRepository placeRepository;

    PlaceReader reader;
    User user;
    District seongbuk;

    // 성북구 중심 (37.5894, 127.0167) 기준 : 위도 0.009 ≈ 1km
    Place near;      // 도서관 · 무료 · 혼자 · 약 1km
    Place far;       // 청년 공간 · 무료 · 혼자 · 약 2km
    Place paid;      // 주민 공간 · 유료 · 혼자 · 약 0.5km
    Place group;     // 공원 · 무료 · 혼자 X · 약 1.5km
    Place expired;   // 주민 공간 · 11-15 끝
    Place outside;   // 공원 · 약 3.2km (네모 안 · 원 밖)

    @BeforeEach
    void setUp() {
        reader = new PlaceReader(userRepository, placeRepository, CLOCK);

        seongbuk = District.of("성북구", new BigDecimal("37.5894"), new BigDecimal("127.0167"), 61, 127);
        ReflectionTestUtils.setField(seongbuk, "id", 1L);

        user = User.createKakaoUser("홍윤지");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        Region stage1 = Region.of(1, "시작의 방", "집 안", "빛의 조각", "#E8C06A", "a.png", "b.png");
        user.completeOnboarding(2, stage1, Goal.of("JOB", "취업"), seongbuk);

        near = place(1L, PlaceCategory.LIBRARY, "가까운 도서관", "37.5984", "127.0167", true, true, null);
        far = place(2L, PlaceCategory.YOUTH_CENTER, "먼 청년 공간", "37.6074", "127.0167", true, true, null);
        paid = place(3L, PlaceCategory.COMMUNITY, "유료 강좌실", "37.5939", "127.0167", false, true, null);
        group = place(4L, PlaceCategory.PARK, "같이 가는 공원", "37.6029", "127.0167", true, false, null);
        expired = place(5L, PlaceCategory.COMMUNITY, "끝난 곳", "37.5900", "127.0167", true, true, LocalDate.of(2026, 11, 15));
        outside = place(6L, PlaceCategory.PARK, "모서리 공원", "37.6104", "127.0420", true, true, null);
    }

    private Place place(long id, PlaceCategory category, String name, String lat, String lng,
                        boolean free, boolean soloOk, LocalDate validTo) {
        Place place = Place.of(category, name, null, "서울", new BigDecimal(lat), new BigDecimal(lng),
                free, soloOk, null, null, null, null, validTo, "예시");
        ReflectionTestUtils.setField(place, "id", id);
        return place;
    }

    private void givenPlaces() {
        given(placeRepository.findActiveNear(any(GeoPoint.class), eq(3_000)))
                .willReturn(List.of(far, near, group, expired, paid, outside));
    }

    private List<String> names(PlacesResponse res) {
        return res.places().stream().map(PlaceItem::name).toList();
    }

    @Test
    @DisplayName("기준 위치 : 보낸 위치 그대로 · 없으면 내 구 중심")
    void originOf() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        GeoPoint mine = new GeoPoint(37.5634, 127.0369);

        assertThat(reader.originOf(USER_ID, mine)).isEqualTo(mine);
        assertThat(reader.originOf(USER_ID, null)).isEqualTo(SEONGBUK);
    }

    @Test
    @DisplayName("시작 체크 전이면 409")
    void originOf_beforeOnboarding() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(User.createKakaoUser("동진")));

        assertThatThrownBy(() -> reader.originOf(USER_ID, null))
                .extracting("errorCode").isEqualTo(ErrorCode.ONBOARDING_REQUIRED);
    }

    @Test
    @DisplayName("거리순 · 기간 지난 곳 · 3km 밖 (네모 모서리) 제외")
    void list_sortedByDistance() {
        givenPlaces();

        PlacesResponse res = reader.list(SEONGBUK, PlaceFilter.none());

        assertThat(names(res)).containsExactly("유료 강좌실", "가까운 도서관", "같이 가는 공원", "먼 청년 공간");
        PlaceItem first = res.places().get(0);
        assertThat(first.distanceM()).isBetween(495, 505);  // 위도 0.0045 ≈ 500m
        assertThat(first.walkMinutes()).isEqualTo(10);      // 500 × 1.3 ÷ 67 ≈ 9.7
    }

    @Test
    @DisplayName("기준을 옮기면 그 기준 거리순 (먼 청년 공간 옆이면 그게 첫 번째)")
    void list_fromMyLocation() {
        givenPlaces();

        PlacesResponse res = reader.list(new GeoPoint(37.6074, 127.0167), PlaceFilter.none());

        assertThat(res.places().get(0).name()).isEqualTo("먼 청년 공간");
        assertThat(res.places().get(0).distanceM()).isZero();
        assertThat(res.places().get(0).walkMinutes()).isEqualTo(1); // 최소 1분
    }

    @Test
    @DisplayName("필터 : 카테고리 · 무료 · 혼자 · 거리")
    void list_filters() {
        givenPlaces();

        assertThat(names(reader.list(SEONGBUK, new PlaceFilter(PlaceCategory.LIBRARY, false, false, null))))
                .containsExactly("가까운 도서관");
        assertThat(names(reader.list(SEONGBUK, new PlaceFilter(null, true, false, null))))
                .doesNotContain("유료 강좌실");
        assertThat(names(reader.list(SEONGBUK, new PlaceFilter(null, false, true, null))))
                .doesNotContain("같이 가는 공원");
        assertThat(names(reader.list(SEONGBUK, new PlaceFilter(null, false, false, 1100))))
                .containsExactly("유료 강좌실", "가까운 도서관");
    }

    @Test
    @DisplayName("주변에 아무것도 없으면 빈 목록")
    void list_empty() {
        given(placeRepository.findActiveNear(any(GeoPoint.class), eq(3_000))).willReturn(List.of());

        assertThat(reader.list(SEONGBUK, PlaceFilter.none()).places()).isEmpty();
    }
}