package com.nagabolkka.backend.domain.plus.controller;

import com.nagabolkka.backend.domain.plus.dto.PlusInterestRequest;
import com.nagabolkka.backend.domain.plus.dto.PlusInterestResponse;
import com.nagabolkka.backend.domain.plus.service.PlusInterestService;
import com.nagabolkka.backend.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "루미 플러스", description = "가격 의향 설문 · 첫 발견 조각")
@RestController
@RequestMapping("/api/plus")
@RequiredArgsConstructor
public class PlusController {

    private final PlusInterestService plusInterestService;

    @Operation(summary = "플러스 관심 · 가격 의향",
            description = "루미 플러스 설문 응답을 저장하거나 바꿉니다. (사용자 당 1개)\n\n"
                    + "- 처음 응답하면 보너스 조각 \"첫 발견 조각\" 을 줍니다. (`rewardPiece`) 응답을 바꾸면 `rewardPiece: null`\n"
                    + "- `CUSTOM` 은 `customPrice` 필수 (0 ~ 50,000원), 다른 옵션의 `customPrice` 는 무시합니다.\n"
                    + "- 보너스 조각은 도감 25칸 · 복원도와 별개입니다. (도감 `bonusPieces`)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "저장 · 수정",
                    content = @Content(schema = @Schema(implementation = PlusInterestResponse.class))),
            @ApiResponse(responseCode = "400", description = "옵션 없음 · 없는 옵션 · CUSTOM 금액 없음 · 범위 밖",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "INVALID_INPUT", value = "{\"code\":\"INVALID_INPUT\",\"message\":\"입력값을 다시 확인해 주세요\"}"),
                            @ExampleObject(name = "PLUS_CUSTOM_PRICE_REQUIRED", value = "{\"code\":\"PLUS_CUSTOM_PRICE_REQUIRED\",\"message\":\"원하는 가격을 0 ~ 50,000원 사이로 적어 주세요\"}")
                    })),
            @ApiResponse(responseCode = "401", description = "토큰 없음 · 만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "UNAUTHORIZED", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"다시 로그인해 주세요\"}"),
                            @ExampleObject(name = "AUTH_TOKEN_EXPIRED", value = "{\"code\":\"AUTH_TOKEN_EXPIRED\",\"message\":\"로그인이 만료됐어요\"}")
                    }))
    })
    @PutMapping("/interest")
    public PlusInterestResponse respond(@Parameter(hidden = true) @AuthenticationPrincipal Long userId,
                                        @Valid @RequestBody PlusInterestRequest request) {
        return plusInterestService.respond(userId, request);
    }
}