package com.nagabolkka.backend.domain.goal.repository;

import com.nagabolkka.backend.domain.goal.entity.Goal;
import com.nagabolkka.backend.domain.goal.entity.GoalStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoalStepRepository extends JpaRepository<GoalStep, Long> {

    // 내 목표에서 사용
    List<GoalStep> findByGoalOrderByStepNoAsc(Goal goal);
}