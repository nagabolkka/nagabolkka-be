package com.nagabolkka.backend.domain.quest.entity;

import com.nagabolkka.backend.domain.mission.entity.Mission;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.global.common.BaseEntity;
import com.nagabolkka.backend.domain.place.entity.Place;
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
import java.math.BigDecimal;

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

    // 동네 지도에서 담은 장소
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    // 배정 당시 장소 이름
    @Column(length = 60)
    private String placeName;

    // 카카오 로컬 장소 (바깥 미션 · 못 찾으면 null) — 장소 좌표이지 사용자 위치 아님
    @Column(length = 20)
    private String kakaoPlaceId;

    @Column(precision = 10, scale = 7)
    private BigDecimal placeLat;

    @Column(precision = 10, scale = 7)
    private BigDecimal placeLng;

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

    // 담기 퀘스트 (오늘 새로 · 내일 예약 배정)
    public static DailyQuest assignPlace(User user, LocalDate questDate, Mission placeMission,
                                         Place place, Integer distanceM) {
        DailyQuest quest = assign(user, questDate, placeMission);
        quest.source = QuestSource.SAVED_PLACE;
        quest.applyPlace(place, distanceM);
        return quest;
    }

    // 오늘 퀘스트를 담은 장소로 교체 : 처음 상태로 · 바꾸기 횟수 그대로
    public void replaceWithPlace(Mission placeMission, Place place, Integer distanceM) {
        checkNotDone();
        replaceMission(placeMission);
        this.source = QuestSource.SAVED_PLACE;
        applyPlace(place, distanceM);
    }

    public boolean isPlace(Long placeId) {
        return place != null && place.getId().equals(placeId);
    }

    // 화면에 보일 제목 · 설명 (담기 퀘스트면 {place} → 장소 이름 · 설명은 장소 설명)
    public String getTitle() {
        return fill(mission.getTitle());
    }

    public String getDescription() {
        if (place != null && place.getDescription() != null) {
            return place.getDescription();
        }
        return fill(mission.getDescription());
    }

    public String fill(String text) {
        return text == null || place == null ? text : text.replace(Mission.PLACE_TOKEN, place.getName());
    }

    private void applyPlace(Place place, Integer distanceM) {
        this.place = place;
        this.placeName = place.getName().length() > 60 ? place.getName().substring(0, 60) : place.getName();
        this.placeLat = place.getLat();
        this.placeLng = place.getLng();
        this.kakaoPlaceId = null;
        this.distanceM = distanceM;
    }

    public int getChangeRemaining() {
        return MAX_CHANGE - changeCount;
    }

    // 실제 장소 찾음 : 이름 · 좌표 · 거리 (사용자 좌표는 저장하지 않음)
    public void foundPlace(String kakaoPlaceId, String placeName, BigDecimal placeLat, BigDecimal placeLng,
                           int distanceM) {
        this.kakaoPlaceId = kakaoPlaceId;
        this.placeName = placeName.length() > 60 ? placeName.substring(0, 60) : placeName;
        this.placeLat = placeLat;
        this.placeLng = placeLng;
        this.distanceM = distanceM;
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
        this.kakaoPlaceId = null;
        this.placeLat = null;
        this.placeLng = null;
        this.place = null;
        this.split = false;
        this.splitDoneSteps = 0;
        this.acceptedAt = null;
    }
}