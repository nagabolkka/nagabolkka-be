package com.nagabolkka.backend.domain.letter.entity;

import com.nagabolkka.backend.domain.letter.service.LetterStats;
import com.nagabolkka.backend.domain.user.entity.User;
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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// 주간 루미의 편지 (사용자 · 주마다 1통)
@Getter
@Entity
@Table(name = "weekly_letters",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "week_start"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeeklyLetter extends BaseEntity {

    public static final int BODY_MAX = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE) // 탈퇴 시 같이 삭제
    private User user;

    // 편지가 다루는 주의 월요일
    @Column(nullable = false)
    private LocalDate weekStart;

    @Column(nullable = false)
    private int outingCount;

    @Column(nullable = false)
    private int splitCount;

    @Column(nullable = false)
    private int pieceCount;

    @Column(nullable = false)
    private int foundSlots;

    @Column(nullable = false, length = BODY_MAX)
    private String body;

    // 추천 장소 id "3,7" (조회 때 다시 읽어 지금 열린 곳만)
    @Column(length = 100)
    private String recommendedPlaceIds;

    public static WeeklyLetter of(User user, LocalDate weekStart, LetterStats stats, String body, List<Long> placeIds) {
        WeeklyLetter letter = new WeeklyLetter();
        letter.user = user;
        letter.weekStart = weekStart;
        letter.outingCount = stats.outingCount();
        letter.splitCount = stats.splitCount();
        letter.pieceCount = stats.pieceCount();
        letter.foundSlots = stats.foundSlots();
        letter.body = body.length() > BODY_MAX ? body.substring(0, BODY_MAX) : body;
        letter.recommendedPlaceIds = placeIds.isEmpty() ? null
                : placeIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        return letter;
    }

    public List<Long> recommendedPlaceIdList() {
        if (recommendedPlaceIds == null || recommendedPlaceIds.isBlank()) {
            return List.of();
        }
        return Arrays.stream(recommendedPlaceIds.split(",")).map(Long::valueOf).toList();
    }

    public String collection() {
        return foundSlots + "/" + LetterStats.TOTAL_SLOTS;
    }
}