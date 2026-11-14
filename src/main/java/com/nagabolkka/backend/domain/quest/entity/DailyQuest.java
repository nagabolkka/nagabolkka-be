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
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;

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

    // 수락 → 진행 중 (이미 진행 중이면 그대로 · 버튼 두 번 눌러도 OK)
    public void accept(LocalDateTime now) {
        checkNotDone();
        if (status == QuestStatus.ASSIGNED) {
            status = QuestStatus.IN_PROGRESS;
            acceptedAt = now;
        }
    }

    // 쪼개기 (수락 포함) — 이미 쪼갰으면 그대로
    public void splitUp(LocalDateTime now) {
        accept(now);
        split = true;
    }

    // 바꾸기 → 새 미션 · 처음 상태로 · 횟수 +1
    public void change(Mission newMission) {
        checkChangeable();
        replaceMission(newMission);
        changeCount++;
    }

    // 오늘은 쉬기 (복원도 · 조각 그대로)
    public void skip() {
        checkNotDone();
        status = QuestStatus.SKIPPED;
    }

    // 완료 : 배정 · 진행 중 → 완료 (수락 없이 바로 완료도 가능)
    public void complete(int doneSteps, LocalDateTime now) {
        checkNotDone();
        if (split && doneSteps < 1) {
            throw new CustomException(ErrorCode.SPLIT_STEP_REQUIRED);
        }
        status = QuestStatus.COMPLETED;
        splitDoneSteps = split ? doneSteps : 0; // 안 쪼갰으면 무시
        completedAt = now;
    }

    // 끝난 퀘스트 = 완료 · 쉬기
    public boolean isDone() {
        return status == QuestStatus.COMPLETED || status == QuestStatus.SKIPPED;
    }

    // 바꾸기 전에 확인 (새 미션 고르기 전에 부른다)
    public void checkChangeable() {
        checkNotDone();
        if (changeCount >= MAX_CHANGE) {
            throw new CustomException(ErrorCode.QUEST_CHANGE_LIMIT);
        }
    }

    private void checkNotDone() {
        if (isDone()) {
            throw new CustomException(ErrorCode.QUEST_ALREADY_DONE);
        }
    }

    // 미션 교체 : 처음 배정된 것처럼 되돌린다
    private void replaceMission(Mission newMission) {
        this.mission = newMission;
        this.region = newMission.getRegion();
        this.source = QuestSource.AUTO;
        this.status = QuestStatus.ASSIGNED;
        this.placeName = newMission.getPlaceName();
        this.distanceM = newMission.isIndoor() ? 0 : null;
        this.split = false;
        this.splitDoneSteps = 0;
        this.acceptedAt = null;
    }
}