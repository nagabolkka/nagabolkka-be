package com.nagabolkka.backend.domain.region.service;

import com.nagabolkka.backend.domain.district.entity.District;
import com.nagabolkka.backend.domain.goal.entity.Goal;
import com.nagabolkka.backend.domain.region.dto.NextRegionRequest;
import com.nagabolkka.backend.domain.region.dto.NextRegionResponse;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.domain.region.entity.RegionStatus;
import com.nagabolkka.backend.domain.region.entity.UserRegion;
import com.nagabolkka.backend.domain.region.repository.UserRegionRepository;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.config.ClockConfig;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RegionServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 14, 15, 0);
    private static final Clock CLOCK = Clock.fixed(
            ZonedDateTime.of(NOW, ClockConfig.KST).toInstant(), ClockConfig.KST);

    @Mock UserRepository userRepository;
    @Mock UserRegionRepository userRegionRepository;

    RegionService regionService;

    User user;
    List<Region> regions;
    List<UserRegion> userRegions;

    @BeforeEach
    void setUp() {
        regionService = new RegionService(userRepository, userRegionRepository, new NextRegionPolicy(), CLOCK);

        regions = IntStream.rangeClosed(1, 5)
                .mapToObj(stage -> {
                    Region region = Region.of(stage, "지역" + stage, "라벨", "조각", "#E8C06A", "a.png", "b.png");
                    ReflectionTestUtils.setField(region, "id", (long) stage);
                    return region;
                })
                .toList();

        // 1계단 출발 · 1계단 복원도 100% (5번 완료) → 2계단 제안 중
        user = User.createKakaoUser("홍윤지");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        user.completeOnboarding(3, regions.get(0), Goal.of("JOB", "취업"),
                District.of("성북구", new BigDecimal("37.5894"), new BigDecimal("127.0167"), 61, 127));

        userRegions = List.of(
                UserRegion.create(user, regions.get(0), RegionStatus.ACTIVE, NOW.minusDays(10)),
                UserRegion.create(user, regions.get(1), RegionStatus.LOCKED, null),
                UserRegion.create(user, regions.get(2), RegionStatus.LOCKED, null),
                UserRegion.create(user, regions.get(3), RegionStatus.LOCKED, null),
                UserRegion.create(user, regions.get(4), RegionStatus.LOCKED, null));
        ReflectionTestUtils.setField(userRegions.get(0), "completedCount", 5);
    }

    private void givenUser() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(userRegionRepository.findByUserIdOrderByRegionStageNoAsc(USER_ID)).willReturn(userRegions);
    }

    @Test
    @DisplayName("열기 → 1계단 COMPLETED · 2계단 ACTIVE · 현재 지역 2계단 · 응답 복원도 0")
    void accept() {
        givenUser();

        NextRegionResponse response = regionService.respondNext(USER_ID, new NextRegionRequest(true));

        assertThat(response.currentRegion().stageNo()).isEqualTo(2);
        assertThat(response.currentRegion().restoration()).isZero();
        assertThat(userRegions.get(0).getStatus()).isEqualTo(RegionStatus.COMPLETED);
        assertThat(userRegions.get(1).getStatus()).isEqualTo(RegionStatus.ACTIVE);
        assertThat(userRegions.get(1).getUnlockedAt()).isEqualTo(NOW);
        assertThat(user.getCurrentRegion()).isSameAs(regions.get(1));
        assertThat(userRegions.get(0).isPassed()).isTrue(); // 쉬운 날 다시 배정 후보
    }

    @Test
    @DisplayName("머물기 → 1계단 그대로 · 제안 시각 저장 · 2계단 LOCKED")
    void stay() {
        givenUser();

        NextRegionResponse response = regionService.respondNext(USER_ID, new NextRegionRequest(false));

        assertThat(response.currentRegion().stageNo()).isEqualTo(1);
        assertThat(response.currentRegion().restoration()).isEqualTo(100);
        assertThat(userRegions.get(0).getLastSuggestedAt()).isEqualTo(NOW);
        assertThat(userRegions.get(1).getStatus()).isEqualTo(RegionStatus.LOCKED);
        assertThat(user.getCurrentRegion()).isSameAs(regions.get(0));
    }

    @Test
    @DisplayName("머물기 후 7일 안 → 409 REGION_NOT_SUGGESTED · 8일 지나면 다시 열 수 있음")
    void afterStay() {
        givenUser();
        userRegions.get(0).stay(NOW.minusDays(3));

        assertThatThrownBy(() -> regionService.respondNext(USER_ID, new NextRegionRequest(true)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.REGION_NOT_SUGGESTED);

        userRegions.get(0).stay(NOW.minusDays(8));
        assertThat(regionService.respondNext(USER_ID, new NextRegionRequest(true)).currentRegion().stageNo())
                .isEqualTo(2);
    }

    @Test
    @DisplayName("복원도 100% 아님 → 409 REGION_NOT_SUGGESTED · 아무것도 안 바뀜")
    void notRestored() {
        ReflectionTestUtils.setField(userRegions.get(0), "completedCount", 4);
        givenUser();

        assertThatThrownBy(() -> regionService.respondNext(USER_ID, new NextRegionRequest(true)))
                .extracting("errorCode").isEqualTo(ErrorCode.REGION_NOT_SUGGESTED);
        assertThat(userRegions.get(0).getStatus()).isEqualTo(RegionStatus.ACTIVE);
        assertThat(user.getCurrentRegion()).isSameAs(regions.get(0));
    }

    @Test
    @DisplayName("시작 체크 전 → 409 ONBOARDING_REQUIRED")
    void beforeOnboarding() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(User.createKakaoUser("동진")));

        assertThatThrownBy(() -> regionService.respondNext(USER_ID, new NextRegionRequest(true)))
                .extracting("errorCode").isEqualTo(ErrorCode.ONBOARDING_REQUIRED);
    }
}