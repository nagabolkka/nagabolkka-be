package com.nagabolkka.backend.domain.user.entity;

import com.nagabolkka.backend.domain.district.entity.District;
import com.nagabolkka.backend.domain.goal.entity.Goal;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.global.common.BaseEntity;
import com.nagabolkka.backend.global.common.Difficulty;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Getter
@Entity
@Table(name = "users") // user 는 H2 · MySQL 예약어라 users 로
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    public static final String DEFAULT_NICKNAME = "탐험가";
    private static final int MAX_NICKNAME_LENGTH = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 표시 이름 (가입 때 변환해서 저장)
    @Column(nullable = false, length = MAX_NICKNAME_LENGTH)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    // 시작 체크 점수 0~24 -> null 이면 아직 온보딩 전
    private Integer startScore;

    // 현재 머무는 지역 (= 계단) · 온보딩 전 null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_region_id")
    private Region currentRegion;

    // 사회 연결 목표 · 온보딩 전 null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_goal_id")
    private Goal currentGoal;

    // 시작 체크 때 고른 구 · 위치 권한 없을 때 대체 기준 · 날씨 격자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id")
    private District district;

    // 다음 날 미션 난이도
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Difficulty difficulty;

    @Column(nullable = false)
    private boolean shareWithAgency;

    private LocalDateTime lastActiveAt;

    // 카카오 첫 로그인 = 가입. 회원번호는 SignUpService 가 user_private_info 에 따로 저장
    public static User createKakaoUser(String kakaoNickname) {
        User user = new User();
        user.nickname = toDisplayName(kakaoNickname);
        user.role = Role.USER;
        user.difficulty = Difficulty.EASY;
        user.shareWithAgency = false;
        return user;
    }

    static String toDisplayName(String kakaoNickname) {
        if (kakaoNickname == null || kakaoNickname.isBlank()) {
            return DEFAULT_NICKNAME;
        }
        String name = kakaoNickname.strip();
        if (name.matches("^[가-힣]{3}$")) {
            return name.substring(1);
        }
        // 이모지처럼 2칸짜리 문자가 잘리지 않도록 code point 기준으로 자른다
        return name.codePoints()
                .limit(MAX_NICKNAME_LENGTH)
                .mapToObj(Character::toString)
                .collect(Collectors.joining());
    }

    // 온보딩 완료 = 시작 체크 점수가 저장됨
    public boolean isOnboardingCompleted() {
        return startScore != null;
    }

    // 시작 체크 결과 저장
    public void completeOnboarding(int startScore, Region startRegion, Goal goal, District district) {
        this.startScore = startScore;
        this.currentRegion = startRegion;
        this.currentGoal = goal;
        this.district = district;
    }

    public void updateLastActiveAt(LocalDateTime now) {
        this.lastActiveAt = now;
    }
}