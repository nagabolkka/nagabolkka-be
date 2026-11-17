package com.nagabolkka.backend.global.batch;

import com.nagabolkka.backend.domain.user.repository.RefreshTokenRepository;
import com.nagabolkka.backend.domain.location.repository.LocationUsageLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupJob {

    // 위치정보법 확인자료 보관 기간
    static final int LOCATION_LOG_MONTHS = 6;

    private final RefreshTokenRepository refreshTokenRepository;
    private final LocationUsageLogRepository locationUsageLogRepository;
    private final Clock clock;

    // 매일 04:00 : 만료된 Refresh 토큰
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void deleteExpiredRefreshTokens() {
        int deleted = refreshTokenRepository.deleteExpired(LocalDateTime.now(clock));
        log.info("만료 Refresh 토큰 정리 {}건", deleted);
    }

    // 매일 04:30 : 6개월 지난 위치 이용 기록
    @Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void deleteOldLocationLogs() {
        int deleted = locationUsageLogRepository.deleteOlderThan(LocalDateTime.now(clock).minusMonths(LOCATION_LOG_MONTHS));
        log.info("위치 이용 기록 정리 {}건", deleted);
    }
}