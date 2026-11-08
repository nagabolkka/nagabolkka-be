package com.nagabolkka.backend.domain.location.entity;

// 위치정보 이용 목적 (ERD enum location_purpose)
public enum LocationPurpose {
    DISTRICT_CHECK, // 시작 체크 (구 판별은 기기 안에서, 서버엔 districtId 만)
    HOME,           // 홈
    QUEST,          // 퀘스트 배정 · 바꾸기
    PLACE_SEARCH    // 동네 지도
}