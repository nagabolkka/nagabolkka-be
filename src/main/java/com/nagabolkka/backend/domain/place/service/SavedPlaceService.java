package com.nagabolkka.backend.domain.place.service;

import com.nagabolkka.backend.domain.place.dto.SavePlaceResponse;
import com.nagabolkka.backend.domain.place.entity.Place;
import com.nagabolkka.backend.domain.place.entity.SavedPlace;
import com.nagabolkka.backend.domain.place.entity.SavedStatus;
import com.nagabolkka.backend.domain.place.repository.PlaceRepository;
import com.nagabolkka.backend.domain.place.repository.SavedPlaceRepository;
import com.nagabolkka.backend.domain.quest.entity.DailyQuest;
import com.nagabolkka.backend.domain.quest.repository.DailyQuestRepository;
import com.nagabolkka.backend.domain.quest.service.PlaceQuestFactory;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import com.nagabolkka.backend.global.geo.GeoPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SavedPlaceService {

    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;
    private final SavedPlaceRepository savedPlaceRepository;
    private final DailyQuestRepository dailyQuestRepository;
    private final PlaceQuestFactory placeQuestFactory;
    private final Clock clock;

    // 담기 : 오늘 퀘스트가 안 끝났으면 바로 교체, 끝났으면 내일 첫 배정
    @Transactional
    public SavePlaceResponse save(Long userId, Long placeId, GeoPoint location) {
        // 홈 · 퀘스트 요청과 겹쳐도 오늘 퀘스트 1개 → 사용자 행 잠금 (QuestService 와 같은 방식)
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        if (!user.isOnboardingCompleted()) {
            throw new CustomException(ErrorCode.ONBOARDING_REQUIRED);
        }

        LocalDate today = LocalDate.now(clock);
        Place place = placeRepository.findById(placeId)
                .filter(p -> p.isOpenOn(today))
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND)); // 없음 · 기간 끝 · 비활성

        Optional<DailyQuest> todayQuest = dailyQuestRepository.findToday(user.getId(), today);

        // 이미 오늘 퀘스트가 그 장소 → 그대로 (두 번 눌러도 진행 상태 유지)
        if (todayQuest.isPresent() && !todayQuest.get().isDone() && todayQuest.get().isPlace(placeId)) {
            return SavePlaceResponse.replacedToday(todayQuest.get().getId());
        }

        // 기다리는 담기는 항상 1개 → 예전 것 취소
        savedPlaceRepository.findByUserIdAndStatus(user.getId(), SavedStatus.WAITING)
                .forEach(SavedPlace::cancel);

        // 오늘 끝남 (완료 · 쉬기) → 내일 첫 퀘스트로
        if (todayQuest.isPresent() && todayQuest.get().isDone()) {
            savedPlaceRepository.save(SavedPlace.waiting(user, place));
            return SavePlaceResponse.reservedTomorrow();
        }

        // 오늘 교체 (없으면 새로) · 바꾸기 횟수 그대로
        GeoPoint origin = location != null ? location : GeoPoint.of(user.getDistrict());
        DailyQuest quest;
        if (todayQuest.isPresent()) {
            quest = todayQuest.get();
            quest.replaceWithPlace(placeQuestFactory.missionFor(user), place,
                    origin == null ? null : origin.distanceTo(place.point()));
        } else {
            quest = placeQuestFactory.create(user, today, place, origin);
        }
        savedPlaceRepository.save(SavedPlace.used(user, place, quest.getId()));
        return SavePlaceResponse.replacedToday(quest.getId());
    }
}