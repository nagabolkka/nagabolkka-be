package com.nagabolkka.backend.domain.safety.repository;

import com.nagabolkka.backend.domain.safety.entity.CrisisEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrisisEventRepository extends JpaRepository<CrisisEvent, Long> {
}