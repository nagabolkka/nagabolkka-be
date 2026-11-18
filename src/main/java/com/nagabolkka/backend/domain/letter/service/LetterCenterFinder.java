package com.nagabolkka.backend.domain.letter.service;

import com.nagabolkka.backend.domain.quest.entity.QuestStatus;
import com.nagabolkka.backend.domain.quest.repository.DailyQuestRepository;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.global.geo.GeoPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

// 편지 추천 기준점 : 그 주에 마지막으로 다녀온 바깥 퀘스트 장소 → 없으면 내 구 중심 (구도 없으면 null)
@Component
@RequiredArgsConstructor
public class LetterCenterFinder {

    private final DailyQuestRepository dailyQuestRepository;

    public GeoPoint centerOf(User user, LocalDate weekStart) {
        return dailyQuestRepository.findPlacePoints(user.getId(), QuestStatus.COMPLETED,
                        weekStart, weekStart.plusDays(6), PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(point -> new GeoPoint(point.lat().doubleValue(), point.lng().doubleValue()))
                .orElseGet(() -> GeoPoint.of(user.getDistrict()));
    }
}