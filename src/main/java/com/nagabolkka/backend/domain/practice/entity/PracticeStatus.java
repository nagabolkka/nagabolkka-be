package com.nagabolkka.backend.domain.practice.entity;

// 연습 세션 상태
public enum PracticeStatus {
    IN_PROGRESS, // 대화 중
    COMPLETED,   // 끝까지 · 피드백 받음
    STOPPED      // 위기 감지로 멈춤
}