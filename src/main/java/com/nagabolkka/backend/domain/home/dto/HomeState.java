package com.nagabolkka.backend.domain.home.dto;

import com.nagabolkka.backend.domain.quest.entity.DailyQuest;
import com.nagabolkka.backend.domain.quest.entity.QuestSource;
import com.nagabolkka.backend.domain.quest.entity.QuestStatus;

// 홈 루미 한 줄 · 버튼 문구를 고르는 상태
public enum HomeState {
    DEFAULT,
    RAINY,
    SAVED_PLACE,
    IN_PROGRESS,
    COMPLETED,
    SKIPPED,
    WELCOME_BACK;

    // 우선순위 : 오늘 퀘스트 결과 (완료 · 건너뜀) > 진행 중 > 오랜만에 돌아옴 > 담은 장소 > 비 > 기본
    public static HomeState of(DailyQuest quest, boolean welcomeBack, boolean rainy) {
        QuestStatus status = quest.getStatus();
        if (status == QuestStatus.COMPLETED) {
            return COMPLETED;
        }
        if (status == QuestStatus.SKIPPED) {
            return SKIPPED;
        }
        if (status == QuestStatus.IN_PROGRESS) {
            return IN_PROGRESS;
        }
        if (welcomeBack) {
            return WELCOME_BACK;
        }
        if (quest.getSource() == QuestSource.SAVED_PLACE) {
            return SAVED_PLACE;
        }
        return rainy ? RAINY : DEFAULT;
    }
}