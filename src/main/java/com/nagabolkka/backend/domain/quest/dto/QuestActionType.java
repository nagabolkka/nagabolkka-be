package com.nagabolkka.backend.domain.quest.dto;

// 오늘 퀘스트 동작
public enum QuestActionType {
    ACCEPT,
    SPLIT,
    CHANGE,
    SKIP,
    REVISIT,
    PRACTICE_MISSION,
    EASIER, // 난이도 한 단계 쉽게 (하루 1번)
    HARDER  // 난이도 한 단계 어렵게 (하루 1번)
}