package com.nagabolkka.backend.domain.region.service;

import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.domain.region.entity.RegionStatus;
import com.nagabolkka.backend.domain.region.entity.UserRegion;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

// 다음 지역 제안 : 현재 지역 복원도 100% · 마지막 계단 아님 · (처음이거나 머물기 후 7일 지남)
@Component
public class NextRegionPolicy {

    // 머물기를 고르면 7일 뒤 다시 제안
    static final int SUGGEST_AGAIN_DAYS = 7;
    private static final int LAST_STAGE = 5;

    public Region suggest(UserRegion current, List<UserRegion> userRegions, LocalDateTime now) {
        int stageNo = current.getRegion().getStageNo();
        if (current.getRestoration() < 100 || stageNo >= LAST_STAGE) {
            return null;
        }
        LocalDateTime lastSuggestedAt = current.getLastSuggestedAt();
        if (lastSuggestedAt != null && lastSuggestedAt.isAfter(now.minusDays(SUGGEST_AGAIN_DAYS))) {
            return null;
        }
        return userRegions.stream()
                .filter(ur -> ur.getRegion().getStageNo() == stageNo + 1)
                .filter(ur -> ur.getStatus() == RegionStatus.LOCKED)
                .map(UserRegion::getRegion)
                .findFirst()
                .orElse(null);
    }
}