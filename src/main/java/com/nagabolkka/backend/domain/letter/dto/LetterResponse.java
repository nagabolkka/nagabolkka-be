package com.nagabolkka.backend.domain.letter.dto;

import com.nagabolkka.backend.domain.letter.entity.WeeklyLetter;
import com.nagabolkka.backend.domain.place.entity.Place;
import com.nagabolkka.backend.domain.place.entity.PlaceCategory;
import com.nagabolkka.backend.global.geo.GeoPoint;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

public record LetterResponse(
        @Schema(description = "편지 주 시작 월요일", example = "2026-11-09") LocalDate weekStart,
        @Schema(description = "받는 사람 (FE 가 「OO에게,」로 그림)", example = "윤지") String nickname,
        @Schema(description = "본문 (문단은 \\n\\n · 인사 · 서명 없음)") String body,
        Stats stats,
        @Schema(description = "추천 장소 0 ~ 2개 (지금 열려 있는 것만)") List<PlaceItem> recommendedPlaces
) {

    public record Stats(
            @Schema(description = "그 주 외출 (완료) 수", example = "3") int outingCount,
            @Schema(description = "그 주 쪼개서 완료한 수", example = "1") int splitCount,
            @Schema(description = "편지 만들 때까지 모은 조각 수", example = "8") int pieceCount,
            @Schema(description = "도감 칸 / 25", example = "6/25") String collection
    ) {
    }

    public record PlaceItem(
            @Schema(example = "3") Long placeId,
            @Schema(example = "아리랑도서관") String name,
            @Schema(description = "LIBRARY · PROGRAM · YOUTH_CENTER · VOLUNTEER", example = "LIBRARY") PlaceCategory category,
            @Schema(description = "구 중심에서 직선거리 (m) · 구 없으면 null", example = "820", nullable = true) Integer distanceM
    ) {
        public static PlaceItem of(Place place, GeoPoint center) {
            Integer distance = center == null ? null : center.distanceTo(place.point());
            return new PlaceItem(place.getId(), place.getName(), place.getCategory(), distance);
        }
    }

    public static LetterResponse of(WeeklyLetter letter, String nickname, List<PlaceItem> places) {
        return new LetterResponse(letter.getWeekStart(), nickname, letter.getBody(),
                new Stats(letter.getOutingCount(), letter.getSplitCount(), letter.getPieceCount(), letter.collection()),
                places);
    }
}