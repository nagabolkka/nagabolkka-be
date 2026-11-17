package com.nagabolkka.backend.domain.quest.repository;

// 관리자 : 사용자 별 배정 (퀘스트가 있는 날) · 완료 · 쉬기 수
public record QuestStats(Long userId, Long assigned, Long completed, Long skipped) {
}