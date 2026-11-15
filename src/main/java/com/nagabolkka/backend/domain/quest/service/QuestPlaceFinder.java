package com.nagabolkka.backend.domain.quest.service;

import com.nagabolkka.backend.domain.mission.entity.Mission;
import com.nagabolkka.backend.domain.quest.entity.DailyQuest;
import com.nagabolkka.backend.global.geo.GeoPoint;
import com.nagabolkka.backend.global.kakao.KakaoLocalClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 바깥 미션의 실제 장소 (카카오 로컬) — 못 찾으면 미션 기본 장소 이름 그대로
@Component
@RequiredArgsConstructor
public class QuestPlaceFinder {

    private final KakaoLocalClient kakaoLocalClient;

    // 계단별 검색 반경 : 문 앞은 가까이, 사회 연결은 넓게
    static int radiusOf(int stageNo) {
        return switch (stageNo) {
            case 1, 2 -> 700;
            case 3 -> 1_500;
            case 4 -> 3_000;
            default -> 5_000;
        };
    }

    // 실내 · 검색어 없음 · 기준 위치 없음 → 그대로
    public void fill(DailyQuest quest, GeoPoint origin) {
        Mission mission = quest.getMission();
        if (mission.isIndoor() || !mission.hasKakaoQuery() || origin == null) {
            return;
        }
        int radius = radiusOf(quest.getRegion().getStageNo());
        kakaoLocalClient.searchNearest(mission.getKakaoQuery(), origin, radius)
                .ifPresent(place -> quest.foundPlace(place.id(), place.name(), place.lat(), place.lng(),
                        origin.distanceTo(place.point())));
    }
}