package com.nagabolkka.backend.global.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

// 카카오 로컬 키워드 검색 응답 중 필요한 부분
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoLocalResponse(List<Document> documents) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Document(
            String id,
            @JsonProperty("place_name") String placeName,
            @JsonProperty("category_name") String categoryName, // 예 : 문화,예술 > 도서관
            @JsonProperty("address_name") String addressName,   // 지번
            @JsonProperty("road_address_name") String roadAddressName,
            String x, // 경도
            String y  // 위도
    ) {
        // 퀘스트용 (기존 테스트 그대로)
        public Document(String id, String placeName, String x, String y) {
            this(id, placeName, null, null, null, x, y);
        }

        private Optional<BigDecimal[]> latLng() {
            if (id == null || placeName == null || x == null || y == null) {
                return Optional.empty();
            }
            try {
                return Optional.of(new BigDecimal[]{
                        new BigDecimal(y).setScale(7, RoundingMode.HALF_UP),
                        new BigDecimal(x).setScale(7, RoundingMode.HALF_UP)});
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }

        // 도로명 주소 먼저 · 없으면 지번
        private String address() {
            return roadAddressName != null && !roadAddressName.isBlank() ? roadAddressName
                    : Objects.requireNonNullElse(addressName, "");
        }
    }

    // 첫 번째 (거리순이면 가장 가까운 곳) · 없거나 좌표가 이상하면 empty — 퀘스트용
    public Optional<KakaoPlace> first() {
        if (documents == null || documents.isEmpty()) {
            return Optional.empty();
        }
        Document doc = documents.get(0);
        return doc.latLng().map(p -> new KakaoPlace(doc.id(), doc.placeName(), p[0], p[1]));
    }

    // 전부 (좌표가 이상한 것만 빼고) — 지도 저장용
    public List<KakaoSearchPlace> all() {
        if (documents == null) {
            return List.of();
        }
        return documents.stream()
                .flatMap(doc -> doc.latLng().stream()
                        .map(p -> new KakaoSearchPlace(doc.id(), doc.placeName(), doc.categoryName(),
                                doc.address(), p[0], p[1])))
                .toList();
    }
}