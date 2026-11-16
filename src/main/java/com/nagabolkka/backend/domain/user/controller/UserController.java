package com.nagabolkka.backend.domain.user.controller;

import com.nagabolkka.backend.domain.user.dto.MyInfoResponse;
import com.nagabolkka.backend.domain.user.dto.UpdateMyInfoRequest;
import com.nagabolkka.backend.domain.user.service.UserService;
import com.nagabolkka.backend.domain.user.service.WithdrawService;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "마이페이지", description = "내 정보 조회 · 목표 변경 · 탈퇴")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final WithdrawService withdrawService;

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

    @Operation(summary = "탈퇴 (내 기록 모두 삭제)",
            description = "내 기록을 모두 지우고 카카오 연결을 끊습니다. 되돌릴 수 없습니다.\n\n"
                    + "- 지워지는 것 : 조각 · 퀘스트 · 기분 · 회고 · 목표 체크 · 담기 · 연습 · 위기 기록 · 로그인 토큰 · 카카오 회원번호\n"
                    + "- 남는 것 : 위치 이용 기록 (위치정보법 확인자료 · 6개월 뒤 자동 삭제)\n"
                    + "- 카카오 연결 끊기가 실패해도 탈퇴는 그대로 진행됩니다.\n"
                    + "- 응답 후 남은 토큰은 401 → 프론트는 토큰을 지우고 처음 화면으로.\n"
                    + "- 관리자 계정은 탈퇴할 수 없습니다. (403)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "탈퇴 완료"),
            @ApiResponse(responseCode = "401", description = "토큰 없음 · 만료 · 이미 탈퇴",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "UNAUTHORIZED", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"다시 로그인해 주세요\"}"),
                            @ExampleObject(name = "AUTH_TOKEN_EXPIRED", value = "{\"code\":\"AUTH_TOKEN_EXPIRED\",\"message\":\"로그인이 만료됐어요\"}")
                    })),
            @ApiResponse(responseCode = "403", description = "관리자 계정",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "FORBIDDEN", value = "{\"code\":\"FORBIDDEN\",\"message\":\"접근 권한이 없어요\"}")))
    })
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        withdrawService.withdraw(userId);
    }
}