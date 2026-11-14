package com.nagabolkka.backend.domain.region.service;

import com.nagabolkka.backend.domain.region.dto.NextRegionRequest;
import com.nagabolkka.backend.domain.region.dto.NextRegionResponse;
import com.nagabolkka.backend.domain.region.entity.Region;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RegionService {

    private final UserRepository userRepository;
    private final UserRegionRepository userRegionRepository;
    private final NextRegionPolicy nextRegionPolicy;
    private final Clock clock;

    // 다음 지역 제안 응답 : 열기 · 머물기
    @Transactional
    public NextRegionResponse respondNext(Long userId, NextRegionRequest request) {
        // 버튼 연타 · 두 화면 동시 요청 → 사용자 행 잠금 (두 번째는 제안 조건이 사라져 409)
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        if (!user.isOnboardingCompleted()) {
            throw new CustomException(ErrorCode.ONBOARDING_REQUIRED);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        List<UserRegion> userRegions = userRegionRepository.findByUserIdOrderByRegionStageNoAsc(userId);
        UserRegion current = findUserRegion(userRegions, user.getCurrentRegion().getId());

        // 홈 카드와 같은 규칙 : 지금 제안 중일 때만
        Region next = nextRegionPolicy.suggest(current, userRegions, now);
        if (next == null) {
            throw new CustomException(ErrorCode.REGION_NOT_SUGGESTED);
        }

        if (!request.accept()) {
            current.stay(now);
            return NextRegionResponse.from(current);
        }

        UserRegion nextRegion = findUserRegion(userRegions, next.getId());
        current.complete();
        nextRegion.unlock(now);
        user.moveTo(next);
        return NextRegionResponse.from(nextRegion);
    }

    private static UserRegion findUserRegion(List<UserRegion> userRegions, Long regionId) {
        return userRegions.stream()
                .filter(ur -> Objects.equals(ur.getRegion().getId(), regionId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("지역 행이 없어요: region " + regionId)); // 데이터 오류 → 500
    }
}