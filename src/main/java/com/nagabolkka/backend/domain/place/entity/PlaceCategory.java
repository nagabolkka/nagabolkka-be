package com.nagabolkka.backend.domain.place.entity;

// 동네 지도 카테고리 — 늘 그 자리에 있는 곳만 (일정 · 기간 있는 프로그램 · 봉사는 X)
public enum PlaceCategory {
    PARK("잠깐 걷거나 벤치에 앉아 있다 오기 좋은 곳이에요."),
    LIBRARY("조용히 책 한 권 펼쳐 보기 좋은 곳이에요."),
    COMMUNITY("게시판에 동네 강좌 · 프로그램 소식이 붙어 있어요."),
    YOUTH_CENTER("청년을 위한 쉼 · 상담 · 모임 공간이에요. 이용 방법은 방문 전 확인해 주세요.");

    // 카카오 장소 설명 (직접 태깅한 시드는 자기 설명)
    private final String defaultDescription;

    PlaceCategory(String defaultDescription) {
        this.defaultDescription = defaultDescription;
    }

    public String defaultDescription() {
        return defaultDescription;
    }
}