package com.nagabolkka.backend.domain.onboarding.service;

import com.nagabolkka.backend.domain.district.entity.District;
import com.nagabolkka.backend.domain.district.repository.DistrictRepository;
import com.nagabolkka.backend.domain.goal.entity.Goal;
import com.nagabolkka.backend.domain.goal.repository.GoalRepository;
import com.nagabolkka.backend.domain.onboarding.dto.StartCheckRequest;
import com.nagabolkka.backend.domain.onboarding.dto.StartCheckResponse;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.domain.region.entity.RegionStatus;
import com.nagabolkka.backend.domain.region.entity.UserRegion;
import com.nagabolkka.backend.domain.region.repository.RegionRepository;
import com.nagabolkka.backend.domain.region.repository.UserRegionRepository;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long DISTRICT_ID = 1L;
    private static final Long GOAL_ID = 3L;

    // 합 10 → 3계단 (동네)
    private static final List<Integer> ANSWERS_SCORE_10 = List.of(2, 1, 0, 1, 0, 1, 2, 3);
    // 합 2 → 1계단 (집 안)
    private static final List<Integer> ANSWERS_SCORE_2 = List.of(0, 0, 1, 0, 0, 1, 0, 0);

    @Mock
    UserRepository userRepository;
    @Mock
    DistrictRepository districtRepository;
    @Mock
    GoalRepository goalRepository;
    @Mock
    RegionRepository regionRepository;
    @Mock
    UserRegionRepository userRegionRepository;

    @InjectMocks
    OnboardingService onboardingService;

    @Captor
    ArgumentCaptor<List<UserRegion>> userRegionsCaptor;

    User user;
    District district;
    List<Region> regions;
    Goal goal;

    @BeforeEach
    void setUp() {
        user = User.createKakaoUser("홍윤지");
        district = District.of("성북구", new BigDecimal("37.5894"), new BigDecimal("127.0167"), 61, 127);
        regions = IntStream.rangeClosed(1, 5)
                .mapToObj(stageNo -> Region.of(stageNo, "지역" + stageNo, "라벨" + stageNo, "조각" + stageNo,
                        "#000000", "img.png", "rain.png"))
                .toList();
        goal = Goal.of("MEETUP", "모임");
    }

    private StartCheckRequest request(List<Integer> answers) {
        return new StartCheckRequest(answers, DISTRICT_ID, GOAL_ID);
    }

    @Test
    @DisplayName("성공 : 점수 10 → 3계단 출발, 지역 5행(1 · 2 PASSED · 3 ACTIVE · 4 · 5 LOCKED), 사용자에 결과 저장")
    void startCheck_success() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(districtRepository.findById(DISTRICT_ID)).willReturn(Optional.of(district));
        given(goalRepository.findById(GOAL_ID)).willReturn(Optional.of(goal));
        given(regionRepository.findAllByOrderByStageNoAsc()).willReturn(regions);

        StartCheckResponse response = onboardingService.startCheck(USER_ID, request(ANSWERS_SCORE_10));

        // 응답
        assertThat(response.score()).isEqualTo(10);
        assertThat(response.region().stageNo()).isEqualTo(3);
        assertThat(response.goal().name()).isEqualTo("모임");

        // user_regions 5행 : 지나온 1 · 2 PASSED, 출발 3 ACTIVE, 위 4 · 5 LOCKED
        then(userRegionRepository).should().saveAll(userRegionsCaptor.capture());
        List<UserRegion> saved = userRegionsCaptor.getValue();
        assertThat(saved).hasSize(5);
        assertThat(saved).extracting(UserRegion::getStatus).containsExactly(
                RegionStatus.PASSED, RegionStatus.PASSED, RegionStatus.ACTIVE, RegionStatus.LOCKED, RegionStatus.LOCKED);
        assertThat(saved.get(2).getUnlockedAt()).isNotNull();   // ACTIVE 만 열린 시각
        assertThat(saved.get(0).getUnlockedAt()).isNull();      // PASSED 는 null

        // users
        assertThat(user.isOnboardingCompleted()).isTrue();
        assertThat(user.getStartScore()).isEqualTo(10);
        assertThat(user.getCurrentRegion()).isSameAs(regions.get(2));
        assertThat(user.getCurrentGoal()).isSameAs(goal);
        assertThat(user.getDistrict()).isSameAs(district);
    }

    @Test
    @DisplayName("1계단 출발 : 지나온 계단 없음 → 1 ACTIVE · 나머지 LOCKED")
    void startCheck_stage1_noPassed() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(districtRepository.findById(DISTRICT_ID)).willReturn(Optional.of(district));
        given(goalRepository.findById(GOAL_ID)).willReturn(Optional.of(goal));
        given(regionRepository.findAllByOrderByStageNoAsc()).willReturn(regions);

        onboardingService.startCheck(USER_ID, request(ANSWERS_SCORE_2));

        then(userRegionRepository).should().saveAll(userRegionsCaptor.capture());
        assertThat(userRegionsCaptor.getValue()).extracting(UserRegion::getStatus).containsExactly(
                RegionStatus.ACTIVE, RegionStatus.LOCKED, RegionStatus.LOCKED, RegionStatus.LOCKED, RegionStatus.LOCKED);
    }

    @Test
    @DisplayName("이미 시작 체크를 마쳤으면 409 ONBOARDING_ALREADY_DONE, 아무것도 저장하지 않음")
    void startCheck_alreadyDone() {
        user.completeOnboarding(2, regions.get(0), goal, district);
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> onboardingService.startCheck(USER_ID, request(ANSWERS_SCORE_10)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ONBOARDING_ALREADY_DONE);

        then(userRegionRepository).should(never()).saveAll(any());
        assertThat(user.getStartScore()).isEqualTo(2); // 그대로
    }

    @Test
    @DisplayName("없는 목표 id → 400 INVALID_INPUT")
    void startCheck_goalNotFound() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(districtRepository.findById(DISTRICT_ID)).willReturn(Optional.of(district));
        given(goalRepository.findById(GOAL_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> onboardingService.startCheck(USER_ID, request(ANSWERS_SCORE_10)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);

        assertThat(user.isOnboardingCompleted()).isFalse();
    }

    @Test
    @DisplayName("없는 구 id → 400 INVALID_INPUT")
    void startCheck_districtNotFound() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(districtRepository.findById(DISTRICT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> onboardingService.startCheck(USER_ID, request(ANSWERS_SCORE_10)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("사용자가 없으면 (탈퇴 직후 등) 401 UNAUTHORIZED")
    void startCheck_userNotFound() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> onboardingService.startCheck(USER_ID, request(ANSWERS_SCORE_10)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.UNAUTHORIZED);
    }
}