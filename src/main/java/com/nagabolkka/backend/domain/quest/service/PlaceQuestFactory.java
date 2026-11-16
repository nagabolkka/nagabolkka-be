package com.nagabolkka.backend.domain.quest.service;

import com.nagabolkka.backend.domain.mission.entity.Mission;
import com.nagabolkka.backend.domain.mission.entity.PlaceType;
import com.nagabolkka.backend.domain.mission.repository.MissionRepository;
import com.nagabolkka.backend.domain.place.entity.Place;
import com.nagabolkka.backend.domain.place.entity.SavedPlace;
import com.nagabolkka.backend.domain.place.entity.SavedStatus;
import com.nagabolkka.backend.domain.place.repository.SavedPlaceRepository;
import com.nagabolkka.backend.domain.quest.entity.DailyQuest;
import com.nagabolkka.backend.domain.quest.repository.DailyQuestRepository;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.global.geo.GeoPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

// 담기 퀘스트 : 현재 계단 담기 미션 + 담은 장소
@Component
@RequiredArgsConstructor
public class PlaceQuestFactory {

    private final MissionRepository missionRepository;
    private final SavedPlaceRepository savedPlaceRepository;
    private final DailyQuestRepository dailyQuestRepository;

    // 현재 계단 담기 미션 (보상 : 2 · 3계단 담기 칸, 1 · 4 · 5계단 도전 칸)
    public Mission missionFor(User user) {
        Long regionId = user.getCurrentRegion().getId();
        return missionRepository.findFirstByRegionIdAndPlaceType(regionId, PlaceType.PLACE_SEED)
                .orElseThrow(() -> new IllegalStateException("담기 미션 시드가 없어요: region " + regionId)); // 시드 누락 → 500
    }

    // 오늘 퀘스트를 담기 퀘스트로 새로 만들기
    public DailyQuest create(User user, LocalDate today, Place place, GeoPoint origin) {
        return dailyQuestRepository.save(
                DailyQuest.assignPlace(user, today, missionFor(user), place, distance(origin, place)));
    }

    // 배정 1순위 : 기다리는 담기 → 오늘 퀘스트 (장소 기간이 끝났으면 취소하고 평소 배정으로)
    public Optional<DailyQuest> assignWaiting(User user, LocalDate today, GeoPoint origin) {
        Optional<SavedPlace> waiting =
                savedPlaceRepository.findFirstByUserIdAndStatusOrderByIdDesc(user.getId(), SavedStatus.WAITING);
        if (waiting.isEmpty()) {
            return Optional.empty();
        }
        SavedPlace saved = waiting.get();
        if (!saved.getPlace().isOpenOn(today)) {
            saved.cancel();
            return Optional.empty();
        }
        DailyQuest quest = create(user, today, saved.getPlace(), origin);
        saved.use(quest.getId());
        return Optional.of(quest);
    }

    // 위치(없으면 구 중심) ↔ 장소 · 구도 없으면 모름
    static Integer distance(GeoPoint origin, Place place) {
        return origin == null ? null : origin.distanceTo(place.point());
    }
}