package com.nagabolkka.backend.domain.piece.controller;

import com.nagabolkka.backend.domain.piece.dto.CollectionResponse;
import com.nagabolkka.backend.domain.piece.service.CollectionService;
import com.nagabolkka.backend.global.exception.ErrorResponse;
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
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "도감", description = "조각 도감")
@RestController
@RequestMapping("/api/collection")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    @Operation(summary = "조각 도감",
            description = "5개 지역 × 5칸 조각 도감과 지역별 복원도, 받은 보너스 조각을 돌려줍니다.\n\n"
                    + "- 찾은 조각 : `name` · `restoreText` · `count`(받은 횟수)\n"
                    + "- 못 찾은 조각 : `name` · `restoreText` 는 null, `hint` 만\n"
                    + "- `foundCount` / `total` 은 지역 칸만 (같은 조각 여러 번 = 1칸)\n"
                    + "- `bonusPieces` 는 도감 25칸 · 복원도와 별개 (없으면 [])")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "도감",
                    content = @Content(schema = @Schema(implementation = CollectionResponse.class))),
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
    public CollectionResponse getCollection(@Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        return collectionService.getCollection(userId);
    }
}