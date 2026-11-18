package com.nagabolkka.backend.domain.quest.service;

import com.nagabolkka.backend.domain.district.entity.District;
import com.nagabolkka.backend.domain.goal.entity.Goal;
import com.nagabolkka.backend.domain.mission.entity.Mission;
import com.nagabolkka.backend.domain.mission.entity.PlaceType;
import com.nagabolkka.backend.domain.mission.repository.MissionRepository;
import com.nagabolkka.backend.domain.piece.entity.Piece;
import com.nagabolkka.backend.domain.piece.entity.PieceType;
import com.nagabolkka.backend.domain.place.entity.Place;
import com.nagabolkka.backend.domain.place.entity.PlaceCategory;
import com.nagabolkka.backend.domain.place.entity.SavedPlace;
import com.nagabolkka.backend.domain.place.entity.SavedStatus;
import com.nagabolkka.backend.domain.place.repository.SavedPlaceRepository;
import com.nagabolkka.backend.domain.quest.entity.DailyQuest;
import com.nagabolkka.backend.domain.quest.entity.QuestSource;
import com.nagabolkka.backend.domain.quest.repository.DailyQuestRepository;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.global.common.Difficulty;
import com.nagabolkka.backend.global.geo.GeoPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PlaceQuestFactoryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 16);
    private static final GeoPoint ORIGIN = new GeoPoint(37.5894, 127.0167);

    @Mock MissionRepository missionRepository;
    @Mock SavedPlaceRepository savedPlaceRepository;
    @Mock DailyQuestRepository dailyQuestRepository;

    PlaceQuestFactory factory;
    User user;
    Region stage2;
    Mission placeMission;

    @BeforeEach
    void setUp() {
        factory = new PlaceQuestFactory(missionRepository, savedPlaceRepository, dailyQuestRepository);

        stage2 = Region.of(2, "바람의 숲", "문 앞", "바람의 조각", "#9DD49A", "a.png", "b.png");
        ReflectionTestUtils.setField(stage2, "id", 2L);
        Piece saved = Piece.of(stage2, 5, "초록 조각", PieceType.SAVED_PLACE, "복원", "힌트");
        placeMission = Mission.placeMission(stage2, saved, Difficulty.NORMAL);

        user = User.createKakaoUser("홍윤지");
        ReflectionTestUtils.setField(user, "id", 1L);
        District seongbuk = District.of("성북구", new BigDecimal("37.5894"), new BigDecimal("127.0167"), 61, 127);
        user.completeOnboarding(5, stage2, Goal.of("JOB", "취업"), seongbuk);
    }

    private Place place(LocalDate validTo) {
        Place place = Place.of(PlaceCategory.COMMUNITY, "북토크", null, "주소",
                new BigDecimal("37.5984"), new BigDecimal("127.0167"), true, true, null, null, null, null, validTo, "예시");
        ReflectionTestUtils.setField(place, "id", 7L);
        return place;
    }

    @Test
    void 기다리는_담기가_없으면_평소_배정() {
        given(savedPlaceRepository.findFirstByUserIdAndStatusOrderByIdDesc(1L, SavedStatus.WAITING))
                .willReturn(Optional.empty());

        assertThat(factory.assignWaiting(user, TODAY, ORIGIN)).isEmpty();
        then(dailyQuestRepository).should(never()).save(any());
    }

    @Test
    void 기다리는_담기가_있으면_오늘_퀘스트_USED() {
        SavedPlace saved = SavedPlace.waiting(user, place(null));
        given(savedPlaceRepository.findFirstByUserIdAndStatusOrderByIdDesc(1L, SavedStatus.WAITING))
                .willReturn(Optional.of(saved));
        given(missionRepository.findFirstByRegionIdAndPlaceType(2L, PlaceType.PLACE_SEED))
                .willReturn(Optional.of(placeMission));
        given(dailyQuestRepository.save(any(DailyQuest.class))).willAnswer(inv -> {
            DailyQuest quest = inv.getArgument(0);
            ReflectionTestUtils.setField(quest, "id", 100L);
            return quest;
        });

        DailyQuest quest = factory.assignWaiting(user, TODAY, ORIGIN).orElseThrow();

        assertThat(quest.getSource()).isEqualTo(QuestSource.SAVED_PLACE);
        assertThat(quest.getTitle()).isEqualTo("북토크 다녀오기");
        assertThat(quest.getDistanceM()).isBetween(995, 1005);   // 위도 0.009 ≈ 1km
        assertThat(saved.getStatus()).isEqualTo(SavedStatus.USED);
        assertThat(saved.getUsedQuestId()).isEqualTo(100L);
    }

    @Test
    void 기간이_끝난_장소면_취소하고_평소_배정() {
        SavedPlace saved = SavedPlace.waiting(user, place(LocalDate.of(2026, 11, 15)));
        given(savedPlaceRepository.findFirstByUserIdAndStatusOrderByIdDesc(1L, SavedStatus.WAITING))
                .willReturn(Optional.of(saved));

        assertThat(factory.assignWaiting(user, TODAY, ORIGIN)).isEmpty();
        assertThat(saved.getStatus()).isEqualTo(SavedStatus.CANCELED);
        then(dailyQuestRepository).should(never()).save(any());
    }
}