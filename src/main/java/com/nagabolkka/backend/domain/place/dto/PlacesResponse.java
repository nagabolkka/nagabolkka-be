package com.nagabolkka.backend.domain.place.dto;

import com.nagabolkka.backend.domain.place.entity.Place;
import com.nagabolkka.backend.domain.place.entity.PlaceCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PlacesResponse(
        @Schema(description = "거리순 (가까운 곳 먼저)") List<PlaceItem> places
) {

    // 도보 시간 = 직선거리 × 1.3 ÷ 67m/분 (오늘 퀘스트와 같은 식, 최소 1분)
    private static final double DETOUR = 1.3;
    private static final double WALK_M_PER_MIN = 67.0;

    public record PlaceItem(
            @Schema(example = "3") Long placeId,
            @Schema(description = "PARK · LIBRARY · COMMUNITY · YOUTH_CENTER", example = "LIBRARY") PlaceCategory category,
            @Schema(example = "아리랑도서관") String name,
            @Schema(example = "서울 성북구 정릉동") String address,
            @Schema(example = "화~일 09:00-22:00 · 월 휴관", description = "카카오에서 찾은 장소는 null", nullable = true) String openHours,
            @Schema(example = "조용한 열람실과 무인 대출기가 있어요.", nullable = true) String description,
            @Schema(description = "내 위치(없으면 내 구 중심) ↔ 장소 직선거리 (m)", example = "650") int distanceM,
            @Schema(example = "13") int walkMinutes,
            @Schema(example = "true") boolean isFree,
            @Schema(description = "혼자 가도 괜찮은 곳", example = "true") boolean soloOk,
            @Schema(example = "37.5950000") BigDecimal lat,
            @Schema(example = "127.0150000") BigDecimal lng,
            @Schema(description = "시작일 (상시면 null)", nullable = true) LocalDate validFrom,
            @Schema(description = "종료일 (상시면 null)", nullable = true) LocalDate validTo
    ) {

        public static PlaceItem of(Place place, int distanceM) {
            int walkMinutes = Math.max(1, (int) Math.round(distanceM * DETOUR / WALK_M_PER_MIN));
            return new PlaceItem(place.getId(), place.getCategory(), place.getName(), place.getAddress(),
                    place.getOpenHours(), place.getDescription(), distanceM, walkMinutes,
                    place.isFree(), place.isSoloOk(), place.getLat(), place.getLng(),
                    place.getValidFrom(), place.getValidTo());
        }
    }
}