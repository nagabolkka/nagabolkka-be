package com.nagabolkka.backend.domain.user.service;

import com.nagabolkka.backend.domain.district.entity.District;
import com.nagabolkka.backend.domain.goal.entity.Goal;
import com.nagabolkka.backend.domain.goal.repository.GoalRepository;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.domain.user.dto.MyInfoResponse;
import com.nagabolkka.backend.domain.user.dto.UpdateMyInfoRequest;
import com.nagabolkka.backend.domain.user.entity.Role;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import com.nagabolkka.backend.domain.piece.repository.PieceAcquisitionRepository;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    UserRepository userRepository;
    @Mock
    GoalRepository goalRepository;
    @Mock
    PieceAcquisitionRepository pieceAcquisitionRepository;

    @InjectMocks
    UserService userService;

    User user;
    Region region;
    Goal meetup;
    Goal job;
    District district;

    @BeforeEach
    void setUp() {
        user = User.createKakaoUser("홍윤지");
        region = Region.of(2, "바람의 숲", "문 앞", "바람의 조각", "#7BC4A8", "img.png", "rain.png");
        meetup = Goal.of("MEETUP", "모임");
        job = Goal.of("JOB", "취업");
        district = District.of("성북구", new BigDecimal("37.5894"), new BigDecimal("127.0167"), 61, 127);
    }

    @Test
    @DisplayName("내 정보 : 시작 체크를 마친 사용자 → 지역 · 목표 · 조각 0 · 도감 0/25")
    void getMyInfo_onboarded() {
        user.completeOnboarding(7, region, meetup, district);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        MyInfoResponse response = userService.getMyInfo(USER_ID);

        assertThat(response.nickname()).isEqualTo("윤지");
        assertThat(response.currentRegion().stageNo()).isEqualTo(2);
        assertThat(response.currentRegion().realLabel()).isEqualTo("문 앞");
        assertThat(response.goal().name()).isEqualTo("모임");
        assertThat(response.pieceCount()).isZero();
        assertThat(response.collection()).isEqualTo("0/25");
        assertThat(response.shareWithAgency()).isFalse();
        assertThat(response.plusInterest()).isNull();
        assertThat(response.role()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("내 정보 : 시작 체크 전 → 지역 · 목표 · 구는 null")
    void getMyInfo_beforeOnboarding() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        MyInfoResponse response = userService.getMyInfo(USER_ID);

        assertThat(response.currentRegion()).isNull();
        assertThat(response.goal()).isNull();
        assertThat(response.districtId()).isNull();
    }

    @Test
    @DisplayName("내 정보 : 사용자가 없으면(탈퇴) 401 UNAUTHORIZED")
    void getMyInfo_userNotFound() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMyInfo(USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("목표 변경 : 모임 → 취업, 응답에도 바뀐 목표")
    void updateGoal_success() {
        user.completeOnboarding(7, region, meetup, district);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(goalRepository.findById(1L)).willReturn(Optional.of(job));

        MyInfoResponse response = userService.updateMyInfo(USER_ID, new UpdateMyInfoRequest(1L));

        assertThat(user.getCurrentGoal()).isSameAs(job);
        assertThat(response.goal().name()).isEqualTo("취업");
        // 다른 값은 그대로
        assertThat(user.getCurrentRegion()).isSameAs(region);
        assertThat(user.getNickname()).isEqualTo("윤지");
    }

    @Test
    @DisplayName("목표 변경 : 없는 목표 id → 400 INVALID_INPUT, 목표는 그대로")
    void updateGoal_unknownGoal() {
        user.completeOnboarding(7, region, meetup, district);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(goalRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateMyInfo(USER_ID, new UpdateMyInfoRequest(99L)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);

        assertThat(user.getCurrentGoal()).isSameAs(meetup);
    }

    @Test
    @DisplayName("내 정보 : 조각 수 · 도감 진행은 조각 기록에서 (8개 · 6/25)")
    void getMyInfo_pieces() {
        ReflectionTestUtils.setField(user, "id", USER_ID);
        user.completeOnboarding(7, region, meetup, district);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(pieceAcquisitionRepository.countByUserId(USER_ID)).willReturn(8L);
        given(pieceAcquisitionRepository.countFoundSlots(USER_ID)).willReturn(6L);

        MyInfoResponse response = userService.getMyInfo(USER_ID);

        assertThat(response.pieceCount()).isEqualTo(8);
        assertThat(response.collection()).isEqualTo("6/25");
    }
}