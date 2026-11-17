package com.nagabolkka.backend.domain.letter.controller;

import com.nagabolkka.backend.domain.letter.dto.LetterResponse;
import com.nagabolkka.backend.domain.letter.service.LetterService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "루미의 편지", description = "매주 월요일 도착하는 편지")
@RestController
@RequestMapping("/api/letters")
@RequiredArgsConstructor
public class LetterController {

    private final LetterService letterService;

    @Operation(summary = "가장 최근 편지",
            description = "매주 월요일 03:00 (KST) 에 지난주 (월 ~ 일) 편지가 만들어집니다.\n\n"
                    + "- 편지가 아직 없으면 **204** (본문 없음)\n"
                    + "- `body` 에는 인사 · 서명이 없습니다 → `nickname` 으로 「OO에게,」 를 그려 주세요.\n"
                    + "- `stats.outingCount` · `splitCount` = 그 주 / `pieceCount` · `collection` = 편지 만들 때까지 누적\n"
                    + "- `recommendedPlaces` : 지금도 열려 있는 장소만 (0 ~ 2개) · `distanceM` = 구 중심 기준")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "편지 있음",
                    content = @Content(schema = @Schema(implementation = LetterResponse.class))),
            @ApiResponse(responseCode = "204", description = "편지 없음 (가입 첫 주 · 시작 체크 전)", content = @Content),
            @ApiResponse(responseCode = "401", description = "토큰 없음 · 만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "UNAUTHORIZED", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"다시 로그인해 주세요\"}"),
                            @ExampleObject(name = "AUTH_TOKEN_EXPIRED", value = "{\"code\":\"AUTH_TOKEN_EXPIRED\",\"message\":\"로그인이 만료됐어요\"}")
                    }))
    })
    @GetMapping("/latest")
    public ResponseEntity<LetterResponse> getLatest(@Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        return letterService.getLatest(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}