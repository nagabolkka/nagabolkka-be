package com.nagabolkka.backend.domain.plus.repository;

import com.nagabolkka.backend.domain.plus.entity.PlusInterest;
import com.nagabolkka.backend.domain.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlusInterestRepository extends JpaRepository<PlusInterest, Long> {

    // 설문 응답 · 내 정보 plusInterest
    Optional<PlusInterest> findByUserId(Long userId);

    // 관리자 : 선택지별 집계 (사용자별 응답은 안 꺼냄)
    @Query("""
            select new com.nagabolkka.backend.domain.plus.repository.PriceOptionCount(
                p.priceOption, count(p), avg(p.customPrice))
            from PlusInterest p
            where p.user.role = :role
            group by p.priceOption
            """)
    List<PriceOptionCount> countByOption(@Param("role") Role role);
}