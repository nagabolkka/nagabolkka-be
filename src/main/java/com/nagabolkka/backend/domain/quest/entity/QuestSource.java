package com.nagabolkka.backend.domain.quest.entity;

// 퀘스트가 어떻게 정해졌는지 (AUTO = 자동 배정 · SAVED_PLACE = 동네 지도 담기 · PRACTICE = 연습 후 실제 미션)
public enum QuestSource {
    AUTO,
    SAVED_PLACE,
    PRACTICE
}