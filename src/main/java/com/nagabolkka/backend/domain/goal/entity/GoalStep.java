package com.nagabolkka.backend.domain.goal.entity;

import com.nagabolkka.backend.global.common.BaseEntity;
import jakarta.persistence.Column;
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

@Getter
@Entity
@Table(name = "goal_steps",
        uniqueConstraints = @UniqueConstraint(columnNames = {"goal_id", "step_no"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoalStep extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    @Column(nullable = false)
    private int stepNo;

    @Column(nullable = false, length = 60)
    private String content;

    public static GoalStep of(Goal goal, int stepNo, String content) {
        GoalStep step = new GoalStep();
        step.goal = goal;
        step.stepNo = stepNo;
        step.content = content;
        return step;
    }
}