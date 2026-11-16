package com.nagabolkka.backend.domain.practice.repository;

import com.nagabolkka.backend.domain.practice.entity.PracticeScenario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PracticeScenarioRepository extends JpaRepository<PracticeScenario, Long> {

    // 목록 : 보상 조각 · 그 계단까지 한 번에
    @EntityGraph(attributePaths = {"rewardPiece", "rewardPiece.region"})
    List<PracticeScenario> findAllByOrderByIdAsc();

    // 시드
    boolean existsByCode(String code);
}