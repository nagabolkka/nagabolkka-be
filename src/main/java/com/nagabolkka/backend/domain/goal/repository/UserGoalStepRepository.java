package com.nagabolkka.backend.domain.goal.repository;

import com.nagabolkka.backend.domain.goal.entity.UserGoalStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserGoalStepRepository extends JpaRepository<UserGoalStep, Long> {

    // 이 목표에서 체크한 단계 id (내 목표 · 단계 체크)
    @Query("select ugs.goalStep.id from UserGoalStep ugs "
            + "where ugs.user.id = :userId and ugs.goalStep.goal.id = :goalId")
    List<Long> findCheckedStepIds(@Param("userId") Long userId, @Param("goalId") Long goalId);

    // 홈 목표 진행도 : 체크한 단계 수
    @Query("select count(ugs) from UserGoalStep ugs "
            + "where ugs.user.id = :userId and ugs.goalStep.goal.id = :goalId")
    long countChecked(@Param("userId") Long userId, @Param("goalId") Long goalId);

    // 해제 = 행 삭제
    void deleteByUserIdAndGoalStepId(Long userId, Long goalStepId);
}