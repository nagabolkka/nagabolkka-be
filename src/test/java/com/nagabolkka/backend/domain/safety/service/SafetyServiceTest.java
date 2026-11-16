package com.nagabolkka.backend.domain.safety.service;

import com.nagabolkka.backend.domain.safety.entity.CrisisEvent;
import com.nagabolkka.backend.domain.safety.entity.CrisisSource;
import com.nagabolkka.backend.domain.safety.repository.CrisisEventRepository;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.config.ClockConfig;
import com.nagabolkka.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class SafetyServiceTest {

    private static final Long USER_ID = 1L;
    private static final Clock CLOCK = Clock.fixed(
            ZonedDateTime.of(2026, 11, 16, 22, 0, 0, 0, ClockConfig.KST).toInstant(), ClockConfig.KST);
    private static final LocalDateTime ONE_MINUTE_AGO = LocalDateTime.of(2026, 11, 16, 21, 59);

    @Mock UserRepository userRepository;
    @Mock CrisisEventRepository crisisEventRepository;

    SafetyService service;
    User user;

    @BeforeEach
    void setUp() {
        service = new SafetyService(userRepository, crisisEventRepository, CLOCK);
        user = User.createKakaoUser("홍윤지");
        ReflectionTestUtils.setField(user, "id", USER_ID);
    }

    @Test
    @DisplayName("도움 받기 직접 열기 → MANUAL 1행 저장")
    void record_manual() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(crisisEventRepository.existsByUserIdAndSourceAndCreatedAtAfter(USER_ID, CrisisSource.MANUAL, ONE_MINUTE_AGO))
                .willReturn(false);

        service.record(USER_ID, CrisisSource.MANUAL);

        ArgumentCaptor<CrisisEvent> captor = ArgumentCaptor.forClass(CrisisEvent.class);
        then(crisisEventRepository).should().save(captor.capture());
        assertThat(captor.getValue().getSource()).isEqualTo(CrisisSource.MANUAL);
        assertThat(captor.getValue().getUser()).isSameAs(user);
    }

    @Test
    @DisplayName("1분 안에 다시 열기 → 저장 안 함 (예외도 없음)")
    void record_duplicate() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(crisisEventRepository.existsByUserIdAndSourceAndCreatedAtAfter(USER_ID, CrisisSource.MANUAL, ONE_MINUTE_AGO))
                .willReturn(true);

        service.record(USER_ID, CrisisSource.MANUAL);

        then(crisisEventRepository).should(never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = CrisisSource.class, names = {"REFLECTION", "PRACTICE"})
    @DisplayName("회고 · 연습 경로 → 400 INVALID_INPUT · 저장 안 함")
    void record_notManual(CrisisSource source) {
        assertThatThrownBy(() -> service.record(USER_ID, source))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
        then(crisisEventRepository).should(never()).save(any());
    }
}