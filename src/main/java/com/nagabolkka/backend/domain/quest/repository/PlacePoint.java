package com.nagabolkka.backend.domain.quest.repository;

import java.math.BigDecimal;

// 퀘스트 장소 좌표 (사용자 위치 아님)
public record PlacePoint(BigDecimal lat, BigDecimal lng) {
}