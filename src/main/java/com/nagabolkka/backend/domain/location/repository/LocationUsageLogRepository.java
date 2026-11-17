package com.nagabolkka.backend.domain.location.repository;

import com.nagabolkka.backend.domain.location.entity.LocationUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface LocationUsageLogRepository extends JpaRepository<LocationUsageLog, Long> {

    // 정리 배치 : 보관 기간 (6개월) 지난 기록 한 번에 삭제 (탈퇴한 사용자 것 포함 · FK 없음)
    @Modifying
    @Query("delete from LocationUsageLog l where l.createdAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);
}