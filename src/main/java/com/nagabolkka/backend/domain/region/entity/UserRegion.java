package com.nagabolkka.backend.domain.region.entity;

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

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "user_regions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "region_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRegion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE) // 탈퇴 시 같이 삭제
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RegionStatus status;

    // 이 지역에서 완료한 퀘스트 수
    @Column(nullable = false)
    private int completedCount;

    // 다음 지역 제안 → 머물기 선택 시각 (1주일 후 다시 제안)
    private LocalDateTime lastSuggestedAt;

    // ACTIVE 가 된 시각
    private LocalDateTime unlockedAt;

    // 시작 체크 : 출발 계단 아래 PASSED · 출발 계단 ACTIVE · 출발 계단 위 LOCKED (OnboardingService 가 상태를 정함)
    public static UserRegion create(User user, Region region, RegionStatus status, LocalDateTime now) {
        UserRegion userRegion = new UserRegion();
        userRegion.user = user;
        userRegion.region = region;
        userRegion.status = status;
        userRegion.completedCount = 0;
        userRegion.unlockedAt = status == RegionStatus.ACTIVE ? now : null;
        return userRegion;
    }

    // 퀘스트 완료 1회 (복원도 +20%, 5번 넘어도 복원도는 100% 에서 멈춤)
    public void addCompletion() {
        completedCount++;
    }

    // 다음 지역으로 넘어감 → 지금 지역은 다 복원한 곳
    public void complete() {
        status = RegionStatus.COMPLETED;
    }

    // 다음 지역 열기 → 지금 머무는 지역
    public void unlock(LocalDateTime now) {
        status = RegionStatus.ACTIVE;
        unlockedAt = now;
    }

    // 다음 지역 제안에서 머물기 → 7일 뒤 다시 제안 (NextRegionPolicy)
    public void stay(LocalDateTime now) {
        lastSuggestedAt = now;
    }

    // 지나온 곳 = 시작 체크 때 건너뛴 계단(PASSED) · 다 복원하고 넘어온 계단(COMPLETED)
    public boolean isPassed() {
        return status == RegionStatus.PASSED || status == RegionStatus.COMPLETED;
    }

    // 복원도 0~100 (퀘스트 완료 1회당 20%, 최대 100)
    public int getRestoration() {
        return Math.min(completedCount, 5) * 20;
    }
}