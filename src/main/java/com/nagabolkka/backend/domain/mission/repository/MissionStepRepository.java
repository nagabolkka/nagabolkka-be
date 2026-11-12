package com.nagabolkka.backend.domain.mission.repository;

import com.nagabolkka.backend.domain.mission.entity.MissionStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MissionStepRepository extends JpaRepository<MissionStep, Long> {

    List<MissionStep> findByMissionIdOrderByStepNoAsc(Long missionId);
}