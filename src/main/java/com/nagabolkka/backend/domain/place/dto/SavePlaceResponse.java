package com.nagabolkka.backend.domain.place.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SavePlaceResponse(
        @Schema(description = "REPLACED_TODAY 오늘 퀘스트 교체 · RESERVED_TOMORROW 내일 첫 퀘스트로 예약",
                example = "REPLACED_TODAY") SaveResult result,
        @Schema(description = "바뀐 오늘 퀘스트 (내일 예약이면 null)", example = "102", nullable = true) Long questId
) {

    public enum SaveResult {
        REPLACED_TODAY,
        RESERVED_TOMORROW
    }

    public static SavePlaceResponse replacedToday(Long questId) {
        return new SavePlaceResponse(SaveResult.REPLACED_TODAY, questId);
    }

    public static SavePlaceResponse reservedTomorrow() {
        return new SavePlaceResponse(SaveResult.RESERVED_TOMORROW, null);
    }
}