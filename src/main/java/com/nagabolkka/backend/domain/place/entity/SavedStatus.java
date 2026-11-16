package com.nagabolkka.backend.domain.place.entity;

// 담기 상태
public enum SavedStatus {
    WAITING,  // 내일 첫 배정 대기 (사용자당 1개)
    USED,     // 퀘스트가 됨
    CANCELED  // 새로 담아서 · 기간이 끝나서 취소
}