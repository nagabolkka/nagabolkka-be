package com.nagabolkka.backend.domain.mission.repository;

import com.nagabolkka.backend.domain.mission.entity.Mission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface MissionRepository extends JpaRepository<Mission, Long> {

    // 배정 후보 : 현재 · 지나온 지역의 활성 미션 (지역 · 보상 조각까지 한 번에)
    @Query("""
            select m from Mission m
            join fetch m.region
            join fetch m.rewardPiece
            where m.region.id in :regionIds and m.active = true
            """)
    List<Mission> findActiveByRegionIds(@Param("regionIds") Collection<Long> regionIds);

    // 시드 : 같은 제목 미션이 이미 있으면 건너뜀 (보상 조각 하나에 미션 여러 개)
    boolean existsByTitle(String title);
}