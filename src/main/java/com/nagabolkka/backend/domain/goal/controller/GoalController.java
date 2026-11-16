package com.nagabolkka.backend.domain.goal.controller;

import com.nagabolkka.backend.domain.goal.dto.CheckGoalStepRequest;
import com.nagabolkka.backend.domain.goal.dto.CheckGoalStepResponse;
import com.nagabolkka.backend.domain.goal.dto.MyGoalResponse;
import com.nagabolkka.backend.domain.goal.service.GoalService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "목표", description = "사회 연결 목표 4단계 · 희망 조각")
@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @Operation(summary = "내 목표",
            description = "현재 목표의 4단계와 체크 상태입니다. hopePieceReceived 는 이 목표로 희망 조각을 받았는지입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = MyGoalResponse.class))),
            @ApiResponse(responseCode = "401", description = "토큰 없음 · 만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "UNAUTHORIZED", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"다시 로그인해 주세요\"}"),
                            @ExampleObject(name = "AUTH_TOKEN_EXPIRED", value = "{\"code\":\"AUTH_TOKEN_EXPIRED\",\"message\":\"로그인이 만료됐어요\"}")
                    })),
            @ApiResponse(responseCode = "404", description = "고른 목표 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "NOT_FOUND", value = "{\"code\":\"NOT_FOUND\",\"message\":\"찾을 수 없어요\"}"))),
            @ApiResponse(responseCode = "409", description = "시작 체크 전",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "ONBOARDING_REQUIRED", value = "{\"code\":\"ONBOARDING_REQUIRED\",\"message\":\"시작 체크를 먼저 해 주세요\"}")))
    })
    @GetMapping("/me")
    public MyGoalResponse getMyGoal(@Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        return goalService.getMyGoal(userId);
    }

    @Operation(summary = "목표 단계 체크 / 해제",
            description = "checked true = 체크, false = 해제입니다. 이 목표를 처음 4/4 완성했을 때만 hopePiece 를 주고, 아니면 null 입니다. "
                    + "해제해도 받은 조각은 회수하지 않습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "체크 · 해제 성공",
                    content = @Content(schema = @Schema(implementation = CheckGoalStepResponse.class))),
            @ApiResponse(responseCode = "400", description = "checked 누락 · 형식 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "INVALID_INPUT", value = "{\"code\":\"INVALID_INPUT\",\"message\":\"입력값을 다시 확인해 주세요\"}"))),
            @ApiResponse(responseCode = "401", description = "토큰 없음 · 만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "UNAUTHORIZED", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"다시 로그인해 주세요\"}"),
                            @ExampleObject(name = "AUTH_TOKEN_EXPIRED", value = "{\"code\":\"AUTH_TOKEN_EXPIRED\",\"message\":\"로그인이 만료됐어요\"}")
                    })),
            @ApiResponse(responseCode = "404", description = "현재 목표에 없는 단계",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "NOT_FOUND", value = "{\"code\":\"NOT_FOUND\",\"message\":\"찾을 수 없어요\"}"))),
            @ApiResponse(responseCode = "409", description = "시작 체크 전",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "ONBOARDING_REQUIRED", value = "{\"code\":\"ONBOARDING_REQUIRED\",\"message\":\"시작 체크를 먼저 해 주세요\"}")))
    })
    @PutMapping("/me/steps/{stepId}")
    public CheckGoalStepResponse checkStep(@Parameter(hidden = true) @AuthenticationPrincipal Long userId,
                                           @Parameter(description = "단계 id (내 목표 steps[].stepId)", example = "1")
                                           @PathVariable Long stepId,
                                           @Valid @RequestBody CheckGoalStepRequest request) {
        return goalService.checkStep(userId, stepId, request.checked());
    }
}