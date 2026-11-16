package com.nagabolkka.backend.domain.plus.repository;

import com.nagabolkka.backend.domain.plus.entity.PlusInterest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlusInterestRepository extends JpaRepository<PlusInterest, Long> {

    // 설문 응답 · 내 정보 plusInterest
    Optional<PlusInterest> findByUserId(Long userId);
}