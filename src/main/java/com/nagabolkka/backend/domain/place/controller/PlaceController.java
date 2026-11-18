package com.nagabolkka.backend.domain.place.controller;

import com.nagabolkka.backend.domain.location.entity.LocationPurpose;
import com.nagabolkka.backend.domain.location.service.LocationLogService;
import com.nagabolkka.backend.domain.place.dto.PlaceFilter;
import com.nagabolkka.backend.domain.place.dto.PlacesResponse;
import com.nagabolkka.backend.domain.place.entity.PlaceCategory;
import com.nagabolkka.backend.domain.place.service.PlaceService;
import com.nagabolkka.backend.global.exception.ErrorResponse;
import com.nagabolkka.backend.global.geo.GeoPoint;
import com.nagabolkka.backend.domain.place.dto.SavePlaceResponse;
import com.nagabolkka.backend.domain.place.service.SavedPlaceService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "동네 지도", description = "동네 장소 목록 · 미션으로 담기")
@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;
    private final SavedPlaceService savedPlaceService;
    private final LocationLogService locationLogService;

    @Operation(summary = "장소 목록",
            description = "내 구의 도서관 · 프로그램 · 청년센터 · 봉사 장소를 가까운 순으로 돌려줍니다. (상세 API 없음 → 목록 데이터로 상세 화면)\n\n"
                    + "- 활성 장소 중 오늘이 `validFrom ~ validTo` 안인 곳만 (null = 상시)\n"
                    + "- 필터 : `category` (없으면 전체) · `free` 무료만 · `solo` 혼자 가능만 · `within` 이 거리 (m) 안만\n"
                    + "- `lat` · `lng` (선택) : 거리 기준. 없으면 내 구 중심 (좌표는 저장하지 않음)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "장소 목록",
                    content = @Content(schema = @Schema(implementation = PlacesResponse.class))),
            @ApiResponse(responseCode = "400", description = "없는 category · 숫자가 아닌 값",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "INVALID_INPUT", value = "{\"code\":\"INVALID_INPUT\",\"message\":\"입력값을 다시 확인해 주세요\"}"))),
            @ApiResponse(responseCode = "401", description = "토큰 없음 · 만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "UNAUTHORIZED", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"다시 로그인해 주세요\"}"),
                            @ExampleObject(name = "AUTH_TOKEN_EXPIRED", value = "{\"code\":\"AUTH_TOKEN_EXPIRED\",\"message\":\"로그인이 만료됐어요\"}")
                    })),
            @ApiResponse(responseCode = "409", description = "시작 체크 전",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "ONBOARDING_REQUIRED", value = "{\"code\":\"ONBOARDING_REQUIRED\",\"message\":\"시작 체크를 먼저 해 주세요\"}")))
    })
    @GetMapping
    public PlacesResponse getPlaces(@Parameter(hidden = true) @AuthenticationPrincipal Long userId,
                                    @Parameter(description = "PARK · LIBRARY · COMMUNITY · YOUTH_CENTER (없으면 전체)")
                                    @RequestParam(required = false) PlaceCategory category,
                                    @Parameter(description = "무료만")
                                    @RequestParam(defaultValue = "false") boolean free,
                                    @Parameter(description = "혼자 가능만")
                                    @RequestParam(defaultValue = "false") boolean solo,
                                    @Parameter(description = "이 거리(m) 안만", example = "1000")
                                    @RequestParam(required = false) Integer within,
                                    @Parameter(description = "현재 위도 (선택)", example = "37.5894")
                                    @RequestParam(required = false) Double lat,
                                    @Parameter(description = "현재 경도 (선택)", example = "127.0167")
                                    @RequestParam(required = false) Double lng) {
        GeoPoint location = GeoPoint.ofNullable(lat, lng);
        if (location != null) {
            locationLogService.record(userId, LocationPurpose.PLACE_SEARCH); // 좌표 없이 이용 사실만
        }
        return placeService.getPlaces(userId, new PlaceFilter(category, free, solo, within), location);
    }

    @Operation(summary = "장소 목록",
            description = "내 위치 주변 3km 의 공원 · 도서관 · 주민 공간 · 청년 공간을 가까운 순으로 돌려줍니다. (상세 API 없음 → 목록 데이터로 상세 화면)\n\n"
                    + "- 처음 가는 동네면 카카오 로컬로 주변 장소를 찾아 저장한 뒤 돌려줍니다 (같은 동네는 7일 동안 다시 찾지 않음)\n"
                    + "- 카카오에서 찾은 장소 : 무료 · 혼자 가능 · `openHours` null · 카테고리 기본 설명\n"
                    + "- 필터 : `category` (없으면 전체) · `free` 무료만 · `solo` 혼자 가능만 · `within` 이 거리 (m) 안만\n"
                    + "- `lat` · `lng` (선택) : 기준 위치. 없으면 내 구 중심 (좌표는 저장하지 않음)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "담기 결과",
                    content = @Content(schema = @Schema(implementation = SavePlaceResponse.class))),
            @ApiResponse(responseCode = "401", description = "토큰 없음 · 만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "UNAUTHORIZED", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"다시 로그인해 주세요\"}"),
                            @ExampleObject(name = "AUTH_TOKEN_EXPIRED", value = "{\"code\":\"AUTH_TOKEN_EXPIRED\",\"message\":\"로그인이 만료됐어요\"}")
                    })),
            @ApiResponse(responseCode = "404", description = "없는 장소 · 기간이 끝난 장소",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "NOT_FOUND", value = "{\"code\":\"NOT_FOUND\",\"message\":\"찾을 수 없어요\"}"))),
            @ApiResponse(responseCode = "409", description = "시작 체크 전",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "ONBOARDING_REQUIRED", value = "{\"code\":\"ONBOARDING_REQUIRED\",\"message\":\"시작 체크를 먼저 해 주세요\"}")))
    })

    @PostMapping("/{placeId}/save")
    public SavePlaceResponse save(@Parameter(hidden = true) @AuthenticationPrincipal Long userId,
                                  @Parameter(description = "장소 id", example = "1") @PathVariable Long placeId,
                                  @Parameter(description = "현재 위도 (선택)", example = "37.5894")
                                  @RequestParam(required = false) Double lat,
                                  @Parameter(description = "현재 경도 (선택)", example = "127.0167")
                                  @RequestParam(required = false) Double lng) {
        GeoPoint location = GeoPoint.ofNullable(lat, lng);
        if (location != null) {
            locationLogService.record(userId, LocationPurpose.QUEST); // 퀘스트 배정에 씀
        }
        return savedPlaceService.save(userId, placeId, location);
    }
}