package com.nagabolkka.backend.domain.letter.service;

import com.nagabolkka.backend.domain.district.entity.District;
import com.nagabolkka.backend.domain.goal.entity.Goal;
import com.nagabolkka.backend.domain.letter.dto.LetterResponse;
import com.nagabolkka.backend.domain.letter.entity.WeeklyLetter;
import com.nagabolkka.backend.domain.letter.repository.WeeklyLetterRepository;
import com.nagabolkka.backend.domain.place.entity.Place;
import com.nagabolkka.backend.domain.place.entity.PlaceCategory;
import com.nagabolkka.backend.domain.place.repository.PlaceRepository;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.config.ClockConfig;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class LetterServiceTest {

    private static final Long USER_ID = 1L;
    private static final Clock CLOCK = Clock.fixed(
            ZonedDateTime.of(2026, 11, 18, 10, 0, 0, 0, ClockConfig.KST).toInstant(), ClockConfig.KST);
    private static final LocalDate WEEK_START = LocalDate.of(2026, 11, 9);

    @Mock UserRepository userRepository;
    @Mock WeeklyLetterRepository weeklyLetterRepository;
    @Mock PlaceRepository placeRepository;

    LetterService service;
    User user;
    District seongbuk;

    @BeforeEach
    void setUp() {
        service = new LetterService(userRepository, weeklyLetterRepository, placeRepository, CLOCK);
        seongbuk = District.of("성북구", new BigDecimal("37.5894"), new BigDecimal("127.0167"), 61, 127);
        Region stage2 = Region.of(2, "바람의 숲", "문 앞", "바람의 조각", "#9DD49A", "a.png", "b.png");
        user = User.createKakaoUser("윤지");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        user.completeOnboarding(5, stage2, Goal.of("JOB", "취업"), seongbuk);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
    }

    private Place place(Long id, String name, LocalDate validTo) {
        Place place = Place.of(PlaceCategory.LIBRARY, name, seongbuk, "주소", new BigDecimal("37.5964"),
                new BigDecimal("127.0167"), true, true, null, null, null, null, validTo, "test");
        ReflectionTestUtils.setField(place, "id", id);
        return place;
    }

    private WeeklyLetter letter(List<Long> placeIds) {
        return WeeklyLetter.of(user, WEEK_START, new LetterStats(3, 1, 8, 6, "바람의 숲", 40, 3.5), "본문", placeIds);
    }

    @Test
    @DisplayName("편지 없음 → empty (컨트롤러 204)")
    void getLatest_none() {
        given(weeklyLetterRepository.findFirstByUserIdOrderByWeekStartDesc(USER_ID)).willReturn(Optional.empty());

        assertThat(service.getLatest(USER_ID)).isEmpty();
        then(placeRepository).should(never()).findAllById(any());
    }

    @Test
    @DisplayName("편지 → 주 · 닉네임 · 본문 · 통계 · 장소 거리")
    void getLatest() {
        given(weeklyLetterRepository.findFirstByUserIdOrderByWeekStartDesc(USER_ID))
                .willReturn(Optional.of(letter(List.of(3L))));
        given(placeRepository.findAllById(List.of(3L))).willReturn(List.of(place(3L, "아리랑도서관", null)));

        LetterResponse response = service.getLatest(USER_ID).orElseThrow();

        assertThat(response.weekStart()).isEqualTo(WEEK_START);
        assertThat(response.nickname()).isEqualTo("윤지");
        assertThat(response.body()).isEqualTo("본문");
        assertThat(response.stats()).isEqualTo(new LetterResponse.Stats(3, 1, 8, "6/25"));
        assertThat(response.recommendedPlaces()).singleElement().satisfies(p -> {
            assertThat(p.placeId()).isEqualTo(3L);
            assertThat(p.category()).isEqualTo(PlaceCategory.LIBRARY);
            assertThat(p.distanceM()).isBetween(750, 800); // 위도 0.007 ≈ 778m
        });
    }

    @Test
    @DisplayName("저장 순서 그대로 · 기간 끝난 장소 · 지워진 장소는 뺌")
    void getLatest_filterClosed() {
        given(weeklyLetterRepository.findFirstByUserIdOrderByWeekStartDesc(USER_ID))
                .willReturn(Optional.of(letter(List.of(7L, 3L, 9L))));
        given(placeRepository.findAllById(List.of(7L, 3L, 9L))).willReturn(List.of(
                place(3L, "도서관", null),
                place(7L, "끝난 프로그램", LocalDate.of(2026, 11, 15))));

        LetterResponse response = service.getLatest(USER_ID).orElseThrow();

        assertThat(response.recommendedPlaces()).extracting(LetterResponse.PlaceItem::placeId).containsExactly(3L);
    }
}