package com.nagabolkka.backend.domain.quest.service;

import com.nagabolkka.backend.domain.mission.entity.Mission;
import com.nagabolkka.backend.domain.piece.entity.Piece;
import com.nagabolkka.backend.domain.piece.entity.PieceType;
import com.nagabolkka.backend.domain.quest.entity.DailyQuest;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.global.common.Difficulty;
import com.nagabolkka.backend.global.geo.GeoPoint;
import com.nagabolkka.backend.global.kakao.KakaoLocalClient;
import com.nagabolkka.backend.global.kakao.KakaoPlace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
public class QuestPlaceFinderTest { // DailyQuestPlaceTest 에서 헬퍼를 쓰려고 public

    private static final GeoPoint HOME = new GeoPoint(37.5894, 127.0167);

    @Mock KakaoLocalClient kakaoLocalClient;

    QuestPlaceFinder finder;

    @BeforeEach
    void setUp() {
        finder = new QuestPlaceFinder(kakaoLocalClient);
    }

    // 계단 · 실내 · 검색어만 바꿔 가며 퀘스트 만들기 (DailyQuestPlaceTest 도 사용)
    public static DailyQuest quest(int stageNo, boolean indoor, String kakaoQuery) {
        Region region = Region.of(stageNo, "지역", "라벨", "조각", "#000000", "a.png", "b.png");
        Piece piece = Piece.of(region, 2, "아침 조각", PieceType.NORMAL, "복원 문구", "힌트");
        Mission mission = Mission.of(region, piece, "편의점에서 음료 하나 사오기", "설명",
                Difficulty.NORMAL, indoor, false, "가까운 편의점", 15);
        mission.changeKakaoQuery(kakaoQuery);
        return DailyQuest.assign(User.createKakaoUser("홍윤지"), LocalDate.of(2026, 11, 15), mission);
    }

    @Test
    void 찾으면_실제_장소와_거리() {
        given(kakaoLocalClient.searchNearest("편의점", HOME, 700)).willReturn(Optional.of(
                new KakaoPlace("123", "GS25 종암점", new BigDecimal("37.5984000"), new BigDecimal("127.0167000"))));
        DailyQuest quest = quest(2, false, "편의점");

        finder.fill(quest, HOME);

        assertThat(quest.getPlaceName()).isEqualTo("GS25 종암점");
        assertThat(quest.getKakaoPlaceId()).isEqualTo("123");
        assertThat(quest.getPlaceLat()).isEqualByComparingTo("37.5984");
        assertThat(quest.getDistanceM()).isCloseTo(1001, within(5));
    }

    @Test
    void 못_찾으면_기본_이름_거리_모름() {
        given(kakaoLocalClient.searchNearest(any(), any(), anyInt())).willReturn(Optional.empty());
        DailyQuest quest = quest(3, false, "도서관");

        finder.fill(quest, HOME);

        assertThat(quest.getPlaceName()).isEqualTo("가까운 편의점");
        assertThat(quest.getDistanceM()).isNull();
        assertThat(quest.getPlaceLat()).isNull();
    }

    @Test
    void 실내_검색어_없음_위치_없음은_검색_안_함() {
        finder.fill(quest(2, true, "편의점"), HOME);
        finder.fill(quest(2, false, null), HOME);
        finder.fill(quest(2, false, "편의점"), null);

        verifyNoInteractions(kakaoLocalClient);
    }

    @ParameterizedTest
    @CsvSource({"1, 700", "2, 700", "3, 1500", "4, 3000", "5, 5000"})
    void 계단별_반경(int stageNo, int radius) {
        assertThat(QuestPlaceFinder.radiusOf(stageNo)).isEqualTo(radius);
    }
}