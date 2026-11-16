package com.nagabolkka.backend.domain.safety.repository;

import com.nagabolkka.backend.domain.safety.entity.CrisisEvent;
import com.nagabolkka.backend.domain.safety.entity.CrisisSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface CrisisEventRepository extends JpaRepository<CrisisEvent, Long> {

    // 도움 받기 직접 열기 : 방금(1분 안) 남긴 기록이 있는지 → 중복 저장 안 함
    boolean existsByUserIdAndSourceAndCreatedAtAfter(Long userId, CrisisSource source, LocalDateTime after);
}