package com.nagabolkka.backend.domain.record.service;

import com.nagabolkka.backend.domain.quest.repository.DailyQuestRepository;
import com.nagabolkka.backend.domain.record.dto.RecordsResponse;
import com.nagabolkka.backend.domain.record.dto.RecordsResponse.DayItem;
import com.nagabolkka.backend.domain.region.entity.UserRegion;
import com.nagabolkka.backend.domain.region.repository.UserRegionRepository;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecordService {

    private final UserRepository userRepository;
    private final DailyQuestRepository dailyQuestRepository;
    private final UserRegionRepository userRegionRepository;
    private final Clock clock;

    // 모험 일지 : 이번 주 (월 ~ 일) · 고른 달 (없으면 이번 달) · 지역 복원도
    public RecordsResponse getRecords(Long userId, YearMonth month) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        if (!user.isOnboardingCompleted()) {
            throw new CustomException(ErrorCode.ONBOARDING_REQUIRED);
        }

        LocalDate today = LocalDate.now(clock); // KST
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        YearMonth target = month == null ? YearMonth.from(today) : month;

        List<DayItem> week = days(userId, weekStart, weekEnd);
        List<DayItem> monthDays = days(userId, target.atDay(1), target.atEndOfMonth());
        List<UserRegion> userRegions = userRegionRepository.findByUserIdOrderByRegionStageNoAsc(userId);

        return RecordsResponse.of(week, monthDays, userRegions);
    }

    private List<DayItem> days(Long userId, LocalDate from, LocalDate to) {
        return RecordsResponse.days(from, to, dailyQuestRepository.findDayRecords(userId, from, to));
    }
}