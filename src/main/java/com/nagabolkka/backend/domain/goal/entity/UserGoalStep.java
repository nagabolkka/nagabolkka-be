package com.nagabolkka.backend.domain.goal.entity;

import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.global.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

// 목표 단계 체크 : 체크 = 행 추가 / 해제 = 행 삭제 (created_at = 체크 시각)
// 목표를 바꿔도 지우지 않음 → 다시 고르면 이어서
@Getter
@Entity
@Table(name = "user_goal_steps",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "goal_step_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserGoalStep extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_step_id", nullable = false)
    private GoalStep goalStep;

    public static UserGoalStep of(User user, GoalStep goalStep) {
        UserGoalStep check = new UserGoalStep();
        check.user = user;
        check.goalStep = goalStep;
        return check;
    }
}