package com.nagabolkka.backend.domain.mission.entity;

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

// 쪼개기 단계 (미션 별 고정 3단계)
@Getter
@Entity
@Table(name = "mission_steps",
        uniqueConstraints = @UniqueConstraint(columnNames = {"mission_id", "step_no"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissionStep extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @Column(nullable = false)
    private int stepNo;

    @Column(nullable = false, length = 60)
    private String content;

    public static MissionStep of(Mission mission, int stepNo, String content) {
        MissionStep step = new MissionStep();
        step.mission = mission;
        step.stepNo = stepNo;
        step.content = content;
        return step;
    }
}