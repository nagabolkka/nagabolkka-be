package com.nagabolkka.backend.domain.record.service;

import com.nagabolkka.backend.domain.district.entity.District;
import com.nagabolkka.backend.domain.goal.entity.Goal;
import com.nagabolkka.backend.domain.quest.entity.QuestStatus;
import com.nagabolkka.backend.domain.quest.repository.DailyQuestRepository;
import com.nagabolkka.backend.domain.quest.repository.DayRecord;
import com.nagabolkka.backend.domain.record.dto.RecordsResponse;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.domain.region.entity.RegionStatus;
import com.nagabolkka.backend.domain.region.entity.UserRegion;
import com.nagabolkka.backend.domain.region.repository.UserRegionRepository;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.config.ClockConfig;
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
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class RecordServiceTest {

    private static final Long USER_ID = 1L;
    // 2026-11-18 (수) 10:00 KST → 이번 주 11-16 (월) ~ 11-22 (일)
    private static final Clock CLOCK = Clock.fixed(
            ZonedDateTime.of(2026, 11, 18, 10, 0, 0, 0, ClockConfig.KST).toInstant(), ClockConfig.KST);
    private static final LocalDate WEEK_START = LocalDate.of(2026, 11, 16);
    private static final LocalDate WEEK_END = LocalDate.of(2026, 11, 22);

    @Mock UserRepository userRepository;
    @Mock DailyQuestRepository dailyQuestRepository;
    @Mock UserRegionRepository userRegionRepository;

    RecordService service;
    User user;
    List<UserRegion> userRegions;

    @BeforeEach
    void setUp() {
        service = new RecordService(userRepository, dailyQuestRepository, userRegionRepository, CLOCK);

        Region stage1 = Region.of(1, "시작의 방", "집 안", "빛의 조각", "#E8C06A", "a.png", "b.png");
        Region stage2 = Region.of(2, "바람의 숲", "문 앞", "바람의 조각", "#9DD49A", "a.png", "b.png");
        user = User.createKakaoUser("홍윤지");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        District seongbuk = District.of("성북구", new BigDecimal("37.5894"), new BigDecimal("127.0167"), 61, 127);
        user.completeOnboarding(5, stage2, Goal.of("JOB", "취업"), seongbuk);

        UserRegion passed = UserRegion.create(user, stage1, RegionStatus.PASSED, null);
        UserRegion active = UserRegion.create(user, stage2, RegionStatus.ACTIVE, null);
        active.addCompletion();
        active.addCompletion(); // 40%
        userRegions = List.of(passed, active);
    }

    @Test
    @DisplayName("month 없음 → 이번 주 7일 · 이번 달 30일 · 완료 / 쉬기 / 진행 중 · 복원도 · 색")
    void getRecords_default() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(dailyQuestRepository.findDayRecords(USER_ID, WEEK_START, WEEK_END)).willReturn(List.of(
                new DayRecord(WEEK_START, QuestStatus.COMPLETED, 4),
                new DayRecord(WEEK_START.plusDays(1), QuestStatus.SKIPPED, null),
                new DayRecord(WEEK_START.plusDays(2), QuestStatus.IN_PROGRESS, null))); // 오늘 진행 중
        given(dailyQuestRepository.findDayRecords(USER_ID, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)))
                .willReturn(List.of(new DayRecord(LocalDate.of(2026, 11, 3), QuestStatus.COMPLETED, 2)));
        given(userRegionRepository.findByUserIdOrderByRegionStageNoAsc(USER_ID)).willReturn(userRegions);

        RecordsResponse res = service.getRecords(USER_ID, null);

        assertThat(res.thisWeek().days()).hasSize(7);
        assertThat(res.thisWeek().days().get(0).date()).isEqualTo(WEEK_START);
        assertThat(res.thisWeek().outingCount()).isEqualTo(1);
        assertThat(res.thisWeek().days().get(1).status()).isEqualTo(QuestStatus.SKIPPED);
        assertThat(res.thisWeek().days().get(2).status()).isNull(); // 진행 중은 아직 표시 안 함

        assertThat(res.month()).hasSize(30);
        assertThat(res.month().get(2).mood()).isEqualTo(2);

        assertThat(res.restoration()).extracting(RecordsResponse.RegionItem::restoration).containsExactly(0, 40);
        assertThat(res.restoration().get(1).color()).isEqualTo("#9DD49A");
    }

    @Test
    @DisplayName("month=2026-02 → 2월 28일 · 이번 주는 그대로 11-16 ~ 11-22")
    void getRecords_pastMonth() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(dailyQuestRepository.findDayRecords(USER_ID, WEEK_START, WEEK_END)).willReturn(List.of());
        given(dailyQuestRepository.findDayRecords(USER_ID, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)))
                .willReturn(List.of());
        given(userRegionRepository.findByUserIdOrderByRegionStageNoAsc(USER_ID)).willReturn(userRegions);

        RecordsResponse res = service.getRecords(USER_ID, YearMonth.of(2026, 2));

        assertThat(res.month()).hasSize(28);
        assertThat(res.month().get(0).date()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(res.thisWeek().days().get(6).date()).isEqualTo(WEEK_END);
        assertThat(res.thisWeek().outingCount()).isZero();
    }

    @Test
    @DisplayName("시작 체크 전 → 409 ONBOARDING_REQUIRED · 조회 안 함")
    void getRecords_onboardingRequired() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(User.createKakaoUser("새 사용자")));

        assertThatThrownBy(() -> service.getRecords(USER_ID, null))
                .extracting("errorCode").isEqualTo(ErrorCode.ONBOARDING_REQUIRED);
        then(dailyQuestRepository).should(never()).findDayRecords(any(), any(), any());
    }
}