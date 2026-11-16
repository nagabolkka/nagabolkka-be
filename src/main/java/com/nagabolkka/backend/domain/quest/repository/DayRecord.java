package com.nagabolkka.backend.domain.quest.repository;

import com.nagabolkka.backend.domain.quest.entity.QuestStatus;

import java.time.LocalDate;

// 모험 일지 : 하루 퀘스트 상태 + 기분 (기록이 없으면 mood null) — 회고 문장은 안 꺼냄
public record DayRecord(LocalDate date, QuestStatus status, Integer mood) {
}