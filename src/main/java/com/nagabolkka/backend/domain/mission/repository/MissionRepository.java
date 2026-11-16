package com.nagabolkka.backend.domain.mission.repository;

import com.nagabolkka.backend.domain.mission.entity.Mission;
import com.nagabolkka.backend.domain.mission.entity.PlaceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    // 담기 : 현재 계단의 담기 미션
    Optional<Mission> findFirstByRegionIdAndPlaceType(Long regionId, PlaceType placeType);

    // 시드 : 계단별 담기 미션이 이미 있는지
    boolean existsByRegionIdAndPlaceType(Long regionId, PlaceType placeType);

    // 시드 : 연습 후 미션이 이미 있으면 그걸 시나리오에 연결
    Optional<Mission> findFirstByTitle(String title);
}