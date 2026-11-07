package com.nagabolkka.backend.domain.user.entity;

// 3개의 항목 모두 필수
public enum ConsentType {
    TERMS,    // 서비스 이용약관
    PRIVACY,  // 개인정보 수집 · 이용
    LOCATION  // 위치기반서비스 이용약관 (GPS)
}