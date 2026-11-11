package com.nagabolkka.backend.domain.user.controller;

import com.nagabolkka.backend.domain.user.dto.MyInfoResponse;
import com.nagabolkka.backend.domain.user.dto.UpdateMyInfoRequest;
import com.nagabolkka.backend.domain.user.service.UserService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "마이페이지", description = "내 정보 조회 · 목표 변경")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 정보",
            description = "마이페이지 화면 정보입니다. 시작 체크 전이면 currentRegion · goal · districtId 가 null 입니다. ")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = MyInfoResponse.class))),
            @ApiResponse(responseCode = "401", description = "토큰 없음 · 만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "UNAUTHORIZED", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"다시 로그인해 주세요\"}"),
                            @ExampleObject(name = "AUTH_TOKEN_EXPIRED", value = "{\"code\":\"AUTH_TOKEN_EXPIRED\",\"message\":\"로그인이 만료됐어요\"}")
                    }))
    })
    @GetMapping("/me")
    public MyInfoResponse getMyInfo(@Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        return userService.getMyInfo(userId);
    }

    @Operation(summary = "목표 변경",
            description = "마이페이지 \"내 목표\" 칩으로 사회 연결 목표를 바꿉니다. 응답은 내 정보와 같은 형태입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공",
                    content = @Content(schema = @Schema(implementation = MyInfoResponse.class))),
            @ApiResponse(responseCode = "400", description = "goalId 누락 · 없는 목표 id",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "INVALID_INPUT", value = "{\"code\":\"INVALID_INPUT\",\"message\":\"입력값을 다시 확인해 주세요\"}"))),
            @ApiResponse(responseCode = "401", description = "토큰 없음 · 만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "UNAUTHORIZED", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"다시 로그인해 주세요\"}"),
                            @ExampleObject(name = "AUTH_TOKEN_EXPIRED", value = "{\"code\":\"AUTH_TOKEN_EXPIRED\",\"message\":\"로그인이 만료됐어요\"}")
                    }))
    })
    @PatchMapping("/me")
    public MyInfoResponse updateMyInfo(@Parameter(hidden = true) @AuthenticationPrincipal Long userId,
                                       @Valid @RequestBody UpdateMyInfoRequest request) {
        return userService.updateMyInfo(userId, request);
    }
}