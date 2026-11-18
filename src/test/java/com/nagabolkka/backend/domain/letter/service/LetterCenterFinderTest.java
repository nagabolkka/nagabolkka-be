package com.nagabolkka.backend.domain.letter.service;

import com.nagabolkka.backend.domain.district.entity.District;
import com.nagabolkka.backend.domain.goal.entity.Goal;
import com.nagabolkka.backend.domain.quest.entity.QuestStatus;
import com.nagabolkka.backend.domain.quest.repository.DailyQuestRepository;
import com.nagabolkka.backend.domain.quest.repository.PlacePoint;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.global.geo.GeoPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LetterCenterFinderTest {

    private static final LocalDate WEEK_START = LocalDate.of(2026, 11, 9);

    @Mock DailyQuestRepository dailyQuestRepository;
    @InjectMocks LetterCenterFinder finder;

    User user;

    @BeforeEach
    void setUp() {
        user = User.createKakaoUser("윤지");
        ReflectionTestUtils.setField(user, "id", 1L);
        Region stage2 = Region.of(2, "바람의 숲", "문 앞", "바람의 조각", "#9DD49A", "a.png", "b.png");
        user.completeOnboarding(5, stage2, Goal.of("JOB", "취업"),
                District.of("성북구", new BigDecimal("37.5894"), new BigDecimal("127.0167"), 61, 127));
    }

    @Test
    @DisplayName("그 주에 마지막으로 다녀온 바깥 퀘스트 장소")
    void centerOf_lastOuting() {
        given(dailyQuestRepository.findPlacePoints(1L, QuestStatus.COMPLETED, WEEK_START, WEEK_START.plusDays(6),
                PageRequest.of(0, 1)))
                .willReturn(List.of(new PlacePoint(new BigDecimal("37.5006000"), new BigDecimal("127.0366000"))));

        assertThat(finder.centerOf(user, WEEK_START)).isEqualTo(new GeoPoint(37.5006, 127.0366));
    }

    @Test
    @DisplayName("바깥 퀘스트가 없던 주 → 내 구 중심")
    void centerOf_none_district() {
        given(dailyQuestRepository.findPlacePoints(1L, QuestStatus.COMPLETED, WEEK_START, WEEK_START.plusDays(6),
                PageRequest.of(0, 1)))
                .willReturn(List.of());

        assertThat(finder.centerOf(user, WEEK_START)).isEqualTo(new GeoPoint(37.5894, 127.0167));
    }
}