package com.nagabolkka.backend.domain.letter.service;

import com.nagabolkka.backend.domain.district.entity.District;
import com.nagabolkka.backend.domain.goal.entity.Goal;
import com.nagabolkka.backend.domain.letter.entity.WeeklyLetter;
import com.nagabolkka.backend.domain.letter.repository.WeeklyLetterRepository;
import com.nagabolkka.backend.domain.piece.repository.PieceAcquisitionRepository;
import com.nagabolkka.backend.domain.place.entity.Place;
import com.nagabolkka.backend.domain.place.entity.PlaceCategory;
import com.nagabolkka.backend.domain.place.repository.PlaceRepository;
import com.nagabolkka.backend.domain.quest.entity.QuestStatus;
import com.nagabolkka.backend.domain.quest.repository.DailyQuestRepository;
import com.nagabolkka.backend.domain.quest.repository.QuestRecordRepository;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.domain.region.entity.RegionStatus;
import com.nagabolkka.backend.domain.region.entity.UserRegion;
import com.nagabolkka.backend.domain.region.repository.UserRegionRepository;
import com.nagabolkka.backend.domain.user.entity.Role;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.ai.AiClient;
import com.nagabolkka.backend.global.config.ClockConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class WeeklyLetterGeneratorTest {

    // 2026-11-16 (월) 03:00 KST → 지난주 11-09 ~ 11-15
    private static final Clock CLOCK = Clock.fixed(
            ZonedDateTime.of(2026, 11, 16, 3, 0, 0, 0, ClockConfig.KST).toInstant(), ClockConfig.KST);
    private static final LocalDate WEEK_START = LocalDate.of(2026, 11, 9);
    private static final LocalDate WEEK_END = LocalDate.of(2026, 11, 15);
    private static final LocalDateTime BEFORE = LocalDateTime.of(2026, 11, 16, 0, 0);

    @Mock UserRepository userRepository;
    @Mock WeeklyLetterRepository weeklyLetterRepository;
    @Mock DailyQuestRepository dailyQuestRepository;
    @Mock QuestRecordRepository questRecordRepository;
    @Mock PieceAcquisitionRepository pieceAcquisitionRepository;
    @Mock UserRegionRepository userRegionRepository;
    @Mock PlaceRepository placeRepository;
    @Mock AiClient aiClient;

    WeeklyLetterGenerator generator;
    District seongbuk;
    Region stage2;

    @BeforeEach
    void setUp() {
        generator = new WeeklyLetterGenerator(userRepository, weeklyLetterRepository, dailyQuestRepository,
                questRecordRepository, pieceAcquisitionRepository, userRegionRepository, placeRepository, aiClient, CLOCK);
        seongbuk = District.of("성북구", new BigDecimal("37.5894"), new BigDecimal("127.0167"), 61, 127);
        ReflectionTestUtils.setField(seongbuk, "id", 10L);
        stage2 = Region.of(2, "바람의 숲", "문 앞", "바람의 조각", "#9DD49A", "a.png", "b.png");
        ReflectionTestUtils.setField(stage2, "id", 2L);
    }

    private User user(Long id, String nickname) {
        User user = User.createKakaoUser(nickname);
        ReflectionTestUtils.setField(user, "id", id);
        user.completeOnboarding(5, stage2, Goal.of("JOB", "취업"), seongbuk);
        return user;
    }

    // 외출 수 · 쪼개기 · 기분 · 조각 · 복원도 40%
    private void givenStats(User user, long outing) {
        Long id = user.getId();
        given(dailyQuestRepository.countByUserIdAndStatusAndQuestDateBetween(id, QuestStatus.COMPLETED, WEEK_START, WEEK_END))
                .willReturn(outing);
        given(dailyQuestRepository.countByUserIdAndStatusAndSplitTrueAndQuestDateBetween(id, QuestStatus.COMPLETED, WEEK_START, WEEK_END))
                .willReturn(outing == 0 ? 0L : 1L);
        if (outing > 0) {
            given(questRecordRepository.averageMood(id, WEEK_START, WEEK_END)).willReturn(3.5);
        }
        given(pieceAcquisitionRepository.countByUserId(id)).willReturn(8L);
        given(pieceAcquisitionRepository.countFoundSlots(id)).willReturn(6L);
        UserRegion active = UserRegion.create(user, stage2, RegionStatus.ACTIVE, null);
        active.addCompletion();
        active.addCompletion();
        given(userRegionRepository.findByUserIdOrderByRegionStageNoAsc(id)).willReturn(List.of(active));
    }

    private Place place(Long id, String name, double lat, boolean soloOk, LocalDate validTo) {
        Place place = Place.of(PlaceCategory.LIBRARY, name, seongbuk, "주소", BigDecimal.valueOf(lat),
                new BigDecimal("127.0167"), true, soloOk, null, null, null, null, validTo, "test");
        ReflectionTestUtils.setField(place, "id", id);
        return place;
    }

    private WeeklyLetter savedLetter() {
        ArgumentCaptor<WeeklyLetter> captor = ArgumentCaptor.forClass(WeeklyLetter.class);
        then(weeklyLetterRepository).should().save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("활동 있는 주 → 통계 · AI 본문 · 추천 장소 저장")
    void generate_ai() {
        User user = user(1L, "윤지");
        given(userRepository.findLetterTargets(Role.USER, BEFORE)).willReturn(List.of(user));
        givenStats(user, 3);
        given(placeRepository.findActiveByDistrictId(10L)).willReturn(List.of(place(3L, "도서관", 37.59, true, null)));
        given(aiClient.weeklyLetter(any())).willReturn(Optional.of(new AiClient.LetterResult("  AI 편지  ", false)));

        WeeklyLetterGenerator.Result result = generator.generate(WEEK_START);

        assertThat(result).isEqualTo(new WeeklyLetterGenerator.Result(WEEK_START, 1, 0, 0));
        WeeklyLetter letter = savedLetter();
        assertThat(letter.getBody()).isEqualTo("AI 편지");
        assertThat(letter.getOutingCount()).isEqualTo(3);
        assertThat(letter.getSplitCount()).isEqualTo(1);
        assertThat(letter.collection()).isEqualTo("6/25");
        assertThat(letter.recommendedPlaceIdList()).containsExactly(3L);

        ArgumentCaptor<AiClient.LetterRequest> request = ArgumentCaptor.forClass(AiClient.LetterRequest.class);
        then(aiClient).should().weeklyLetter(request.capture());
        assertThat(request.getValue()).isEqualTo(
                new AiClient.LetterRequest("윤지", 3, 1, 8, 6, "바람의 숲", 40, 3.5));
    }

    @Test
    @DisplayName("쉬어간 주 (외출 0) → AI 안 부르고 템플릿")
    void generate_zero_template() {
        User user = user(1L, "윤지");
        given(userRepository.findLetterTargets(Role.USER, BEFORE)).willReturn(List.of(user));
        givenStats(user, 0);
        given(placeRepository.findActiveByDistrictId(10L)).willReturn(List.of());

        generator.generate(WEEK_START);

        then(aiClient).should(never()).weeklyLetter(any());
        then(questRecordRepository).should(never()).averageMood(any(), any(), any());
        assertThat(savedLetter().getBody()).endsWith("이번 주는 조용히 쉬어갔구나. 그것도 괜찮아.");
    }

    @Test
    @DisplayName("AI 서버 실패 (empty) → 같은 숫자로 템플릿")
    void generate_aiFail_template() {
        User user = user(1L, "윤지");
        given(userRepository.findLetterTargets(Role.USER, BEFORE)).willReturn(List.of(user));
        givenStats(user, 2);
        given(placeRepository.findActiveByDistrictId(10L)).willReturn(List.of());
        given(aiClient.weeklyLetter(any())).willReturn(Optional.empty());

        generator.generate(WEEK_START);

        assertThat(savedLetter().getBody()).startsWith("이번 주엔 2번 바깥에 다녀왔어.");
    }

    @Test
    @DisplayName("이미 그 주 편지가 있음 → 건너뜀 (통계 · AI 안 부름)")
    void generate_exists_skip() {
        User user = user(1L, "윤지");
        given(userRepository.findLetterTargets(Role.USER, BEFORE)).willReturn(List.of(user));
        given(weeklyLetterRepository.existsByUserIdAndWeekStart(1L, WEEK_START)).willReturn(true);

        WeeklyLetterGenerator.Result result = generator.generate(WEEK_START);

        assertThat(result.skipped()).isEqualTo(1);
        then(weeklyLetterRepository).should(never()).save(any());
        then(aiClient).should(never()).weeklyLetter(any());
    }

    @Test
    @DisplayName("저장 중 유니크 충돌 → 건너뜀")
    void generate_duplicate_skip() {
        User user = user(1L, "윤지");
        given(userRepository.findLetterTargets(Role.USER, BEFORE)).willReturn(List.of(user));
        givenStats(user, 0);
        given(placeRepository.findActiveByDistrictId(10L)).willReturn(List.of());
        given(weeklyLetterRepository.save(any())).willThrow(new DataIntegrityViolationException("uk"));

        assertThat(generator.generate(WEEK_START)).isEqualTo(new WeeklyLetterGenerator.Result(WEEK_START, 0, 1, 0));
    }

    @Test
    @DisplayName("한 명이 실패해도 다음 사람은 만들어짐")
    void generate_oneFails_continue() {
        User broken = user(1L, "실패");
        User ok = user(2L, "성공");
        given(userRepository.findLetterTargets(Role.USER, BEFORE)).willReturn(List.of(broken, ok));
        given(dailyQuestRepository.countByUserIdAndStatusAndQuestDateBetween(1L, QuestStatus.COMPLETED, WEEK_START, WEEK_END))
                .willThrow(new IllegalStateException("db"));
        givenStats(ok, 0);
        given(placeRepository.findActiveByDistrictId(10L)).willReturn(List.of());

        WeeklyLetterGenerator.Result result = generator.generate(WEEK_START);

        assertThat(result).isEqualTo(new WeeklyLetterGenerator.Result(WEEK_START, 1, 0, 1));
        then(weeklyLetterRepository).should(times(1)).save(any());
    }

    @Test
    @DisplayName("추천 : 닫힌 · 혼자 어려운 장소 빼고 가까운 순 2곳")
    void recommend_filterAndSort() {
        User user = user(1L, "윤지");
        given(placeRepository.findActiveByDistrictId(10L)).willReturn(List.of(
                place(1L, "먼 곳", 37.70, true, null),
                place(2L, "가까운 곳", 37.59, true, null),
                place(3L, "기간 끝", 37.5894, true, LocalDate.of(2026, 11, 1)),
                place(4L, "같이 가야 함", 37.5894, false, null),
                place(5L, "중간", 37.62, true, null)));

        // 장소 3곳 → 2026-11-09 주 offset = (20401 / 7 * 2) % 3
        List<Long> ids = generator.recommend(user, WEEK_START);

        assertThat(ids).hasSize(2).doesNotContain(3L, 4L).isSubsetOf(1L, 2L, 5L);
    }

    @Test
    @DisplayName("추천 : 다음 주엔 다른 조합 (가까운 6곳 안에서 돌아감)")
    void recommend_rotates() {
        User user = user(1L, "윤지");
        given(placeRepository.findActiveByDistrictId(10L)).willReturn(List.of(
                place(1L, "a", 37.590, true, null), place(2L, "b", 37.591, true, null),
                place(3L, "c", 37.592, true, null), place(4L, "d", 37.593, true, null)));

        List<Long> thisWeek = generator.recommend(user, WEEK_START);
        List<Long> nextWeek = generator.recommend(user, WEEK_START.plusWeeks(1));

        assertThat(thisWeek).hasSize(2);
        assertThat(nextWeek).hasSize(2).isNotEqualTo(thisWeek);
    }
}