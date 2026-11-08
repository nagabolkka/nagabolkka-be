package com.nagabolkka.backend.domain.location.repository;

import com.nagabolkka.backend.domain.location.entity.LocationUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationUsageLogRepository extends JpaRepository<LocationUsageLog, Long> {
}