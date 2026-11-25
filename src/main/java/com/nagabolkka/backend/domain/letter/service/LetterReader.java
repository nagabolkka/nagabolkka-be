package com.nagabolkka.backend.domain.letter.service;

import com.nagabolkka.backend.domain.letter.dto.LetterResponse;
import com.nagabolkka.backend.domain.letter.dto.LetterResponse.PlaceItem;
import com.nagabolkka.backend.domain.letter.entity.WeeklyLetter;
import com.nagabolkka.backend.domain.letter.repository.WeeklyLetterRepository;
import com.nagabolkka.backend.domain.place.entity.Place;
import com.nagabolkka.backend.domain.place.repository.PlaceRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LetterReader {

    private final UserRepository userRepository;
    private final WeeklyLetterRepository weeklyLetterRepository;
    private final PlaceRepository placeRepository;
    private final LetterCenterFinder letterCenterFinder;
    private final LetterRecommender letterRecommender;
    private final Clock clock;

    // 가장 최근 편지 · 없으면 empty → 컨트롤러가 204
    // location 있으면 지금 위치 근처 장소 · 없으면 편지를 만들 때 고른 장소
    public Optional<LetterResponse> getLatest(Long userId, GeoPoint location) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        return weeklyLetterRepository.findFirstByUserIdOrderByWeekStartDesc(userId)
                .map(letter -> LetterResponse.of(letter, user.getNickname(),
                        location != null ? nearby(location, letter) : places(user, letter)));
    }

    // 지금 위치 3km · 지금 열린 · 혼자 OK · 주마다 돌아가며 2곳 (같은 주에는 같은 결과)
    private List<PlaceItem> nearby(GeoPoint location, WeeklyLetter letter) {
        return letterRecommender.pick(location, letter.getWeekStart()).stream()
                .map(place -> PlaceItem.of(place, location))
                .toList();
    }

    // 위치 없음 : 저장된 순서 그대로 · 지금 닫힌 장소는 뺌
    private List<PlaceItem> places(User user, WeeklyLetter letter) {
        List<Long> ids = letter.recommendedPlaceIdList();
        if (ids.isEmpty()) {
            return List.of();
        }
        LocalDate today = LocalDate.now(clock);
        Map<Long, Place> byId = placeRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Place::getId, Function.identity()));
        GeoPoint center = letterCenterFinder.centerOf(user, letter.getWeekStart()); // 만들 때와 같은 기준점
        return ids.stream()
                .map(byId::get)
                .filter(place -> place != null && place.isOpenOn(today))
                .map(place -> PlaceItem.of(place, center))
                .toList();
    }
}