package com.nagabolkka.backend.domain.goal.repository;

import com.nagabolkka.backend.domain.goal.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    boolean existsByCode(String code);
}