package com.nagabolkka.backend.domain.quest.entity;

import com.nagabolkka.backend.domain.mission.entity.Mission;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

// 사용자 별 하루 퀘스트 (하루 1개)
@Getter
@Entity
@Table(name = "daily_quests",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "quest_date"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyQuest extends BaseEntity {

    // 바꾸기는 하루 2번까지 (담기 · 연습 교체는 미포함)
    public static final int MAX_CHANGE = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    // KST 기준 날짜
    @Column(nullable = false)
    private LocalDate questDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    // 미션의 지역 (지나온 계단 미션이면 그 계단) → 완료 시 이 지역 복원도가 오른다
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private QuestSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private QuestStatus status;

    // 배정 당시 장소 이름
    @Column(length = 60)
    private String placeName;

    // 사용자 위치 ↔ 장소 직선거리 (사용자 좌표는 저장 안 함) · 실내 0 · 모르면 null
    private Integer distanceM;

    @Column(name = "is_split", nullable = false)
    private boolean split;

    @Column(nullable = false)
    private int splitDoneSteps;

    @Column(nullable = false)
    private int changeCount;

    private LocalDateTime acceptedAt;

    private LocalDateTime completedAt;

    // 자동 배정
    public static DailyQuest assign(User user, LocalDate questDate, Mission mission) {
        DailyQuest quest = new DailyQuest();
        quest.user = user;
        quest.questDate = questDate;
        quest.mission = mission;
        quest.region = mission.getRegion();
        quest.source = QuestSource.AUTO;
        quest.status = QuestStatus.ASSIGNED;
        quest.placeName = mission.getPlaceName();
        quest.distanceM = mission.isIndoor() ? 0 : null;
        quest.split = false;
        quest.splitDoneSteps = 0;
        quest.changeCount = 0;
        return quest;
    }

    public int getChangeRemaining() {
        return MAX_CHANGE - changeCount;
    }
}