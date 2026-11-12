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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final UserRepository userRepository;
    private final DistrictRepository districtRepository;
    private final GoalRepository goalRepository;
    private final RegionRepository regionRepository;
    private final UserRegionRepository userRegionRepository;

    @Transactional
    public StartCheckResponse startCheck(Long userId, StartCheckRequest request) {
        // 1) 사용자
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

        // 2) 재검사는 지원하지 않음
        if (user.isOnboardingCompleted()) {
            throw new CustomException(ErrorCode.ONBOARDING_ALREADY_DONE);
        }

        // 3) 구 · 목표
        District district = districtRepository.findById(request.districtId())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));
        Goal goal = goalRepository.findById(request.goalId())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));

        // 4) 점수 → 출발 계단
        int score = StartCheckScore.sum(request.answers());
        int startStageNo = StartCheckScore.toStageNo(score);

        List<Region> regions = regionRepository.findAllByOrderByStageNoAsc();
        Region startRegion = regions.stream()
                .filter(region -> region.getStageNo() == startStageNo)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("지역 시드가 없어요: " + startStageNo)); // 시드 누락 = 버그 → 500

        // 5) 지역 5행 : 출발 아래 PASSED · 출발 ACTIVE · 위 LOCKED
        LocalDateTime now = LocalDateTime.now();
        userRegionRepository.saveAll(regions.stream()
                .map(region -> UserRegion.create(user, region, statusOf(region, startRegion), now))
                .toList());

        // 6) 사용자에 결과 저장 (변경 감지로 UPDATE)
        user.completeOnboarding(score, startRegion, goal, district);

        return StartCheckResponse.of(score, startRegion, goal);
    }

    // 지나온 계단은 PASSED → 쉬운 날 그 계단 미션도 배정
    private static RegionStatus statusOf(Region region, Region startRegion) {
        if (region.getStageNo() < startRegion.getStageNo()) {
            return RegionStatus.PASSED;
        }
        return region == startRegion ? RegionStatus.ACTIVE : RegionStatus.LOCKED;
    }
}