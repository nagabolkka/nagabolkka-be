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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    private static final Long USER_ID = 1L;
    private static final Clock CLOCK = Clock.fixed(
            ZonedDateTime.of(2026, 11, 16, 10, 0, 0, 0, ClockConfig.KST).toInstant(), ClockConfig.KST);

    @Mock UserRepository userRepository;
    @Mock PlaceRepository placeRepository;

    PlaceService placeService;
    User user;
    District seongbuk;

    // 성북구 중심 (37.5894, 127.0167) 기준 : 위도 0.009 ≈ 1km
    Place near;      // 도서관 · 무료 · 혼자 · 약 1km
    Place far;       // 청년센터 · 무료 · 혼자 · 약 2km
    Place paid;      // 프로그램 · 유료 · 혼자 · 약 0.5km
    Place group;     // 봉사 · 무료 · 혼자 X · 약 1.5km
    Place expired;   // 프로그램 · 11-15 끝

    @BeforeEach
    void setUp() {
        placeService = new PlaceService(userRepository, placeRepository, CLOCK);

        seongbuk = District.of("성북구", new BigDecimal("37.5894"), new BigDecimal("127.0167"), 61, 127);
        ReflectionTestUtils.setField(seongbuk, "id", 1L);

        user = User.createKakaoUser("홍윤지");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        Region stage1 = Region.of(1, "시작의 방", "집 안", "빛의 조각", "#E8C06A", "a.png", "b.png");
        user.completeOnboarding(2, stage1, Goal.of("JOB", "취업"), seongbuk);

        near = place(1L, PlaceCategory.LIBRARY, "가까운 도서관", "37.5984", true, true, null);
        far = place(2L, PlaceCategory.YOUTH_CENTER, "먼 청년센터", "37.6074", true, true, null);
        paid = place(3L, PlaceCategory.PROGRAM, "유료 클래스", "37.5939", false, true, null);
        group = place(4L, PlaceCategory.VOLUNTEER, "같이 하는 봉사", "37.6029", true, false, null);
        expired = place(5L, PlaceCategory.PROGRAM, "끝난 프로그램", "37.5900", true, true, LocalDate.of(2026, 11, 15));
    }

    private Place place(long id, PlaceCategory category, String name, String lat,
                        boolean free, boolean soloOk, LocalDate validTo) {
        Place place = Place.of(category, name, seongbuk, "서울 성북구", new BigDecimal(lat), new BigDecimal("127.0167"),
                free, soloOk, null, null, null, null, validTo, "예시");
        ReflectionTestUtils.setField(place, "id", id);
        return place;
    }

    private void givenPlaces() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(placeRepository.findActiveByDistrictId(1L)).willReturn(List.of(far, near, group, expired, paid));
    }

    private List<String> names(PlacesResponse res) {
        return res.places().stream().map(PlaceItem::name).toList();
    }

    @Test
    @DisplayName("위치 없으면 구 중심 기준 거리순 · 기간 지난 프로그램 제외")
    void sortedByDistance() {
        givenPlaces();

        PlacesResponse res = placeService.getPlaces(USER_ID, PlaceFilter.none(), null);

        assertThat(names(res)).containsExactly("유료 클래스", "가까운 도서관", "같이 하는 봉사", "먼 청년센터");
        PlaceItem first = res.places().get(0);
        assertThat(first.distanceM()).isBetween(495, 505);                 // 위도 0.0045 ≈ 500m
        assertThat(first.walkMinutes()).isEqualTo(10);                     // 500 × 1.3 ÷ 67 ≈ 9.7
    }

    @Test
    @DisplayName("내 위치를 보내면 그 기준 (먼 청년센터 옆이면 그게 첫 번째)")
    void fromMyLocation() {
        givenPlaces();

        PlacesResponse res = placeService.getPlaces(USER_ID, PlaceFilter.none(), new GeoPoint(37.6074, 127.0167));

        assertThat(res.places().get(0).name()).isEqualTo("먼 청년센터");
        assertThat(res.places().get(0).distanceM()).isZero();
        assertThat(res.places().get(0).walkMinutes()).isEqualTo(1);       // 최소 1분
    }

    @Test
    @DisplayName("필터 : 카테고리 · 무료 · 혼자 · 거리")
    void filters() {
        givenPlaces();

        assertThat(names(placeService.getPlaces(USER_ID, new PlaceFilter(PlaceCategory.LIBRARY, false, false, null), null)))
                .containsExactly("가까운 도서관");
        assertThat(names(placeService.getPlaces(USER_ID, new PlaceFilter(null, true, false, null), null)))
                .doesNotContain("유료 클래스");
        assertThat(names(placeService.getPlaces(USER_ID, new PlaceFilter(null, false, true, null), null)))
                .doesNotContain("같이 하는 봉사");
        assertThat(names(placeService.getPlaces(USER_ID, new PlaceFilter(null, false, false, 1100), null)))
                .containsExactly("유료 클래스", "가까운 도서관");
    }

    @Test
    @DisplayName("시작 체크 전이면 409 (장소 조회 안 함)")
    void beforeOnboarding() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(User.createKakaoUser("동진")));

        assertThatThrownBy(() -> placeService.getPlaces(USER_ID, PlaceFilter.none(), null))
                .extracting("errorCode").isEqualTo(ErrorCode.ONBOARDING_REQUIRED);
        then(placeRepository).should(never()).findActiveByDistrictId(any());
    }
}