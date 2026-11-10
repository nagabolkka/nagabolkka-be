package com.nagabolkka.backend.domain.location.service;

import com.nagabolkka.backend.domain.location.entity.LocationPurpose;
import com.nagabolkka.backend.domain.location.entity.LocationUsageLog;
import com.nagabolkka.backend.domain.location.repository.LocationUsageLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationLogService {

    private final LocationUsageLogRepository locationUsageLogRepository;

    public void record(Long userId, LocationPurpose purpose) {
        try {
            locationUsageLogRepository.save(LocationUsageLog.of(userId, purpose));
        } catch (Exception e) {
            log.warn("위치 이용 기록 실패 userId={} purpose={}", userId, purpose, e);
        }
    }
}