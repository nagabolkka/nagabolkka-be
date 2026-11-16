package com.nagabolkka.backend.domain.place.service;

import com.nagabolkka.backend.domain.district.entity.District;
import com.nagabolkka.backend.domain.goal.entity.Goal;
import com.nagabolkka.backend.domain.mission.entity.Mission;
import com.nagabolkka.backend.domain.piece.entity.Piece;
import com.nagabolkka.backend.domain.piece.entity.PieceType;
import com.nagabolkka.backend.domain.place.dto.SavePlaceResponse;
import com.nagabolkka.backend.domain.place.dto.SavePlaceResponse.SaveResult;
import com.nagabolkka.backend.domain.place.entity.Place;
import com.nagabolkka.backend.domain.place.entity.PlaceCategory;
import com.nagabolkka.backend.domain.place.entity.SavedPlace;
import com.nagabolkka.backend.domain.place.entity.SavedStatus;
import com.nagabolkka.backend.domain.place.repository.PlaceRepository;
import com.nagabolkka.backend.domain.place.repository.SavedPlaceRepository;
import com.nagabolkka.backend.domain.quest.entity.DailyQuest;
import com.nagabolkka.backend.domain.quest.entity.QuestSource;
import com.nagabolkka.backend.domain.quest.entity.QuestStatus;
import com.nagabolkka.backend.domain.quest.repository.DailyQuestRepository;
import com.nagabolkka.backend.domain.quest.service.PlaceQuestFactory;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.common.Difficulty;
import com.nagabolkka.backend.global.config.ClockConfig;
import com.nagabolkka.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class SavedPlaceServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 16);
    private static final Clock CLOCK = Clock.fixed(
            ZonedDateTime.of(2026, 11, 16, 10, 0, 0, 0, ClockConfig.KST).toInstant(), ClockConfig.KST);

    @Mock UserRepository userRepository;
    @Mock PlaceRepository placeRepository;
    @Mock SavedPlaceRepository savedPlaceRepository;
    @Mock DailyQuestRepository dailyQuestRepository;
    @Mock PlaceQuestFactory placeQuestFactory;

    SavedPlaceService service;
    User user;
    Place library;
    Mission normal;
    Mission placeMission;

    @BeforeEach
    void setUp() {
        service = new SavedPlaceService(userRepository, placeRepository, savedPlaceRepository,
                dailyQuestRepository, placeQuestFactory, CLOCK);

        Region stage2 = Region.of(2, "바람의 숲", "문 앞", "바람의 조각", "#9DD49A", "a.png", "b.png");
        Piece easy = Piece.of(stage2, 1, "바람 조각", PieceType.EASY, "복원", "힌트");
        Piece saved = Piece.of(stage2, 5, "초록 조각", PieceType.SAVED_PLACE, "복원", "힌트");
        normal = Mission.of(stage2, easy, "집 앞 골목 한 바퀴", "설명", Difficulty.EASY, false, false, "집 앞 골목", 10);
        placeMission = Mission.placeMission(stage2, saved, Difficulty.NORMAL);

        user = User.createKakaoUser("홍윤지");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        District seongbuk = District.of("성북구", new BigDecimal("37.5894"), new BigDecimal("127.0167"), 61, 127);
        user.completeOnboarding(5, stage2, Goal.of("JOB", "취업"), seongbuk);

        library = Place.of(PlaceCategory.LIBRARY, "아리랑도서관", seongbuk, "주소",
                new BigDecimal("37.5984"), new BigDecimal("127.0167"), true, true, null, null, null, null, null, "카카오맵 확인");
        ReflectionTestUtils.setField(library, "id", 1L);

        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
    }

    private DailyQuest todayQuest() {
        DailyQuest quest = DailyQuest.assign(user, TODAY, normal);
        ReflectionTestUtils.setField(quest, "id", 50L);
        return quest;
    }

    @Test
    @DisplayName("진행 중 퀘스트 → 담은 장소로 교체 · USED 기록 · 바꾸기 횟수 그대로")
    void replaceInProgress() {
        DailyQuest quest = todayQuest();
        quest.accept(LocalDateTime.of(2026, 11, 16, 9, 0));
        given(placeRepository.findById(1L)).willReturn(Optional.of(library));
        given(dailyQuestRepository.findToday(USER_ID, TODAY)).willReturn(Optional.of(quest));
        given(savedPlaceRepository.findByUserIdAndStatus(USER_ID, SavedStatus.WAITING)).willReturn(List.of());
        given(placeQuestFactory.missionFor(user)).willReturn(placeMission);

        SavePlaceResponse res = service.save(USER_ID, 1L, null);

        assertThat(res.result()).isEqualTo(SaveResult.REPLACED_TODAY);
        assertThat(res.questId()).isEqualTo(50L);
        assertThat(quest.getSource()).isEqualTo(QuestSource.SAVED_PLACE);
        assertThat(quest.getStatus()).isEqualTo(QuestStatus.ASSIGNED);
        assertThat(quest.getChangeRemaining()).isEqualTo(2);
        assertThat(quest.getDistanceM()).isBetween(995, 1005);     // 구 중심 기준

        ArgumentCaptor<SavedPlace> captor = ArgumentCaptor.forClass(SavedPlace.class);
        then(savedPlaceRepository).should().save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SavedStatus.USED);
        assertThat(captor.getValue().getUsedQuestId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("오늘 퀘스트 완료 → 내일 예약 · 예전 대기는 취소")
    void reserveTomorrow() {
        DailyQuest quest = todayQuest();
        quest.complete(0, LocalDateTime.of(2026, 11, 16, 9, 0));
        SavedPlace old = SavedPlace.waiting(user, library);
        given(placeRepository.findById(1L)).willReturn(Optional.of(library));
        given(dailyQuestRepository.findToday(USER_ID, TODAY)).willReturn(Optional.of(quest));
        given(savedPlaceRepository.findByUserIdAndStatus(USER_ID, SavedStatus.WAITING)).willReturn(List.of(old));

        SavePlaceResponse res = service.save(USER_ID, 1L, null);

        assertThat(res.result()).isEqualTo(SaveResult.RESERVED_TOMORROW);
        assertThat(res.questId()).isNull();
        assertThat(old.getStatus()).isEqualTo(SavedStatus.CANCELED);
        ArgumentCaptor<SavedPlace> captor = ArgumentCaptor.forClass(SavedPlace.class);
        then(savedPlaceRepository).should().save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SavedStatus.WAITING);
    }

    @Test
    @DisplayName("오늘 퀘스트가 없음 → 담기 퀘스트를 새로")
    void createToday() {
        DailyQuest created = DailyQuest.assignPlace(user, TODAY, placeMission, library, 1000);
        ReflectionTestUtils.setField(created, "id", 60L);
        given(placeRepository.findById(1L)).willReturn(Optional.of(library));
        given(dailyQuestRepository.findToday(USER_ID, TODAY)).willReturn(Optional.empty());
        given(savedPlaceRepository.findByUserIdAndStatus(USER_ID, SavedStatus.WAITING)).willReturn(List.of());
        given(placeQuestFactory.create(eq(user), eq(TODAY), eq(library), any())).willReturn(created);

        SavePlaceResponse res = service.save(USER_ID, 1L, null);

        assertThat(res.result()).isEqualTo(SaveResult.REPLACED_TODAY);
        assertThat(res.questId()).isEqualTo(60L);
    }

    @Test
    @DisplayName("이미 오늘 퀘스트가 그 장소 → 그대로 (진행 상태 유지 · 기록 안 늘어남)")
    void samePlaceTwice() {
        DailyQuest quest = DailyQuest.assignPlace(user, TODAY, placeMission, library, 1000);
        ReflectionTestUtils.setField(quest, "id", 70L);
        quest.accept(LocalDateTime.of(2026, 11, 16, 9, 0));
        given(placeRepository.findById(1L)).willReturn(Optional.of(library));
        given(dailyQuestRepository.findToday(USER_ID, TODAY)).willReturn(Optional.of(quest));

        SavePlaceResponse res = service.save(USER_ID, 1L, null);

        assertThat(res.questId()).isEqualTo(70L);
        assertThat(quest.getStatus()).isEqualTo(QuestStatus.IN_PROGRESS);
        then(savedPlaceRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("없는 장소 · 기간 끝 → 404")
    void notFound() {
        given(placeRepository.findById(9L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(USER_ID, 9L, null))
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
    }
}