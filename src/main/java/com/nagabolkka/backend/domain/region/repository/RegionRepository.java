package com.nagabolkka.backend.domain.region.repository;

import com.nagabolkka.backend.domain.region.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, Long> {

    Optional<Region> findByStageNo(int stageNo);

    boolean existsByStageNo(int stageNo);

    // 1 → 5 계단 순서
    List<Region> findAllByOrderByStageNoAsc();
}