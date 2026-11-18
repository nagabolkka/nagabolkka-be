package com.nagabolkka.backend.global.kakao;

import java.math.BigDecimal;

// 카카오 로컬 검색 결과 (지도 저장용 · 카테고리 · 주소 포함)
public record KakaoSearchPlace(String id, String name, String categoryName, String address,
                               BigDecimal lat, BigDecimal lng) {
}