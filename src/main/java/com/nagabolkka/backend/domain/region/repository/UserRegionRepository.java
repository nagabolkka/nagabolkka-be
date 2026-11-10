package com.nagabolkka.backend.domain.region.repository;

import com.nagabolkka.backend.domain.region.entity.UserRegion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRegionRepository extends JpaRepository<UserRegion, Long> {

    // 내 지역 5개 (홈 · 도감에서 사용) — user.id 로 찾고 region.stageNo 순서
    List<UserRegion> findByUserIdOrderByRegionStageNoAsc(Long userId);
}