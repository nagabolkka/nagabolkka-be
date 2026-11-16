package com.nagabolkka.backend.domain.quest.entity;

import com.nagabolkka.backend.domain.mission.entity.Mission;
import com.nagabolkka.backend.domain.piece.entity.Piece;
import com.nagabolkka.backend.domain.piece.entity.PieceType;
import com.nagabolkka.backend.domain.place.entity.Place;
import com.nagabolkka.backend.domain.place.entity.PlaceCategory;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.global.common.Difficulty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DailyQuestPlaceSaveTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 16);

    Region stage2;
    Mission normal;       // 평소 미션
    Mission placeMission; // 담기 미션
    Place library;
    User user;

    @BeforeEach
    void setUp() {
        stage2 = Region.of(2, "바람의 숲", "문 앞", "바람의 조각", "#9DD49A", "a.png", "b.png");
        Piece easy = Piece.of(stage2, 1, "바람 조각", PieceType.EASY, "복원", "힌트");
        Piece saved = Piece.of(stage2, 5, "초록 조각", PieceType.SAVED_PLACE, "복원", "힌트");
        normal = Mission.of(stage2, easy, "집 앞 골목 한 바퀴", "설명", Difficulty.EASY, false, false, "집 앞 골목", 10);
        placeMission = Mission.placeMission(stage2, saved, Difficulty.NORMAL);
        library = Place.of(PlaceCategory.LIBRARY, "아리랑도서관", null, "서울 성북구 아리랑로 82",
                new BigDecimal("37.5998972"), new BigDecimal("127.0139784"), true, true, null,
                "조용한 열람실이 있어요.", null, null, null, "카카오맵 확인");
        ReflectionTestUtils.setField(library, "id", 1L);
        user = User.createKakaoUser("홍윤지");
    }

    @Test
    void 담기_퀘스트는_장소_이름_제목_장소_설명() {
        DailyQuest quest = DailyQuest.assignPlace(user, TODAY, placeMission, library, 1200);

        assertThat(quest.getSource()).isEqualTo(QuestSource.SAVED_PLACE);
        assertThat(quest.getTitle()).isEqualTo("아리랑도서관 다녀오기");
        assertThat(quest.getDescription()).isEqualTo("조용한 열람실이 있어요.");
        assertThat(quest.fill("{place} 입구까지 가기")).isEqualTo("아리랑도서관 입구까지 가기");
        assertThat(quest.getPlaceName()).isEqualTo("아리랑도서관");
        assertThat(quest.getDistanceM()).isEqualTo(1200);
        assertThat(quest.isPlace(1L)).isTrue();
    }

    @Test
    void 일반_퀘스트는_미션_문구_그대로() {
        DailyQuest quest = DailyQuest.assign(user, TODAY, normal);

        assertThat(quest.getTitle()).isEqualTo("집 앞 골목 한 바퀴");
        assertThat(quest.getDescription()).isEqualTo("설명");
        assertThat(quest.isPlace(1L)).isFalse();
    }

    @Test
    void 진행_중에_담기로_교체하면_처음_상태_바꾸기_횟수_그대로() {
        DailyQuest quest = DailyQuest.assign(user, TODAY, normal);
        quest.accept(LocalDateTime.of(2026, 11, 16, 10, 0));

        quest.replaceWithPlace(placeMission, library, 500);

        assertThat(quest.getStatus()).isEqualTo(QuestStatus.ASSIGNED);
        assertThat(quest.getSource()).isEqualTo(QuestSource.SAVED_PLACE);
        assertThat(quest.getChangeRemaining()).isEqualTo(2);
        assertThat(quest.getTitle()).isEqualTo("아리랑도서관 다녀오기");
    }

    @Test
    void 담기_퀘스트를_바꾸면_장소가_지워짐() {
        DailyQuest quest = DailyQuest.assignPlace(user, TODAY, placeMission, library, 1200);

        quest.change(normal);

        assertThat(quest.getPlace()).isNull();
        assertThat(quest.getSource()).isEqualTo(QuestSource.AUTO);
        assertThat(quest.getTitle()).isEqualTo("집 앞 골목 한 바퀴");
        assertThat(quest.getChangeRemaining()).isEqualTo(1);
    }
}