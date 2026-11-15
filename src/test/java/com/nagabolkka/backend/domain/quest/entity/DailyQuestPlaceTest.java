package com.nagabolkka.backend.domain.quest.entity;

import com.nagabolkka.backend.domain.quest.service.QuestPlaceFinderTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DailyQuestPlaceTest {

    @Test
    void 바꾸면_찾은_장소가_지워짐() {
        DailyQuest quest = QuestPlaceFinderTest.quest(2, false, "편의점");
        quest.foundPlace("123", "GS25 종암점", new BigDecimal("37.5921"), new BigDecimal("127.0321"), 210);

        quest.change(QuestPlaceFinderTest.quest(2, false, "빵집").getMission());

        assertThat(quest.getKakaoPlaceId()).isNull();
        assertThat(quest.getPlaceLat()).isNull();
        assertThat(quest.getPlaceLng()).isNull();
        assertThat(quest.getDistanceM()).isNull();
        assertThat(quest.getPlaceName()).isEqualTo("가까운 편의점"); // 새 미션 기본 이름 (헬퍼가 같은 이름)
    }

    @Test
    void 긴_장소_이름은_60자로() {
        DailyQuest quest = QuestPlaceFinderTest.quest(2, false, "편의점");

        quest.foundPlace("1", "가".repeat(70), new BigDecimal("37.5"), new BigDecimal("127.0"), 100);

        assertThat(quest.getPlaceName()).hasSize(60);
    }
}