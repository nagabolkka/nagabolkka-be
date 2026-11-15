package com.nagabolkka.backend.global.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

// 카카오 로컬 키워드 검색 응답 중 필요한 부분 (documents[].id · place_name · x · y)
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoLocalResponse(List<Document> documents) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Document(
            String id,
            @JsonProperty("place_name") String placeName,
            String x, // 경도
            String y  // 위도
    ) {
    }

    // 첫 번째 (거리순이면 가장 가까운 곳) · 없거나 좌표가 이상하면 empty
    public Optional<KakaoPlace> first() {
        if (documents == null || documents.isEmpty()) {
            return Optional.empty();
        }
        Document doc = documents.get(0);
        if (doc.id() == null || doc.placeName() == null || doc.x() == null || doc.y() == null) {
            return Optional.empty();
        }
        try {
            BigDecimal lat = new BigDecimal(doc.y()).setScale(7, RoundingMode.HALF_UP);
            BigDecimal lng = new BigDecimal(doc.x()).setScale(7, RoundingMode.HALF_UP);
            return Optional.of(new KakaoPlace(doc.id(), doc.placeName(), lat, lng));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}