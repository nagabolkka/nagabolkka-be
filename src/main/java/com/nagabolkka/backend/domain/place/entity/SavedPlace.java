package com.nagabolkka.backend.domain.place.entity;

import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

// 동네 지도에서 담은 장소 (created_at = 담은 시각)
@Getter
@Entity
@Table(name = "saved_places", indexes = @Index(name = "idx_saved_places_user_status", columnList = "user_id, status"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SavedPlace extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SavedStatus status;

    // 퀘스트가 된 daily_quests.id (ERD 대로 FK 없음 → 탈퇴 삭제 순서 문제 없음)
    private Long usedQuestId;

    // 내일 배정 대기
    public static SavedPlace waiting(User user, Place place) {
        SavedPlace saved = new SavedPlace();
        saved.user = user;
        saved.place = place;
        saved.status = SavedStatus.WAITING;
        return saved;
    }

    // 바로 오늘 퀘스트가 됨
    public static SavedPlace used(User user, Place place, Long questId) {
        SavedPlace saved = waiting(user, place);
        saved.use(questId);
        return saved;
    }

    public void use(Long questId) {
        this.status = SavedStatus.USED;
        this.usedQuestId = questId;
    }

    public void cancel() {
        this.status = SavedStatus.CANCELED;
    }
}