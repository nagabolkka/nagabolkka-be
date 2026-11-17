package com.nagabolkka.backend.domain.admin.controller;

import com.nagabolkka.backend.domain.admin.dto.AdminUsersResponse;
import com.nagabolkka.backend.domain.admin.service.AdminUserService;
import com.nagabolkka.backend.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 ㅡ 사용자 조회", description = "사용자 현황 (ADMIN)")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "사용자 현황",
            description = "요약 (전체 · 평균 달성률 · 플러스 관심) + 사용자 별 배정 · 완료 · 쉬기 · 달성률 · 마지막 접속입니다.\n\n"
                    + "- 이름은 마스킹 (윤* · 홍*지 · 남**지) 하며, 카카오 정보는 내려주지 않습니다.\n"
                    + "- 달성률 = 완료 ÷ 퀘스트가 배정된 날 · 평균은 배정이 1번 이상인 사용자 기준입니다.\n"
                    + "- 관리자 계정은 제외 · 기분 · 회고 · 연습 · 위기 기록은 조회하지 않습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = AdminUsersResponse.class))),
            @ApiResponse(responseCode = "401", description = "토큰 없음 · 만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "UNAUTHORIZED", value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"다시 로그인해 주세요\"}"))),
            @ApiResponse(responseCode = "403", description = "관리자 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples =
                    @ExampleObject(name = "FORBIDDEN", value = "{\"code\":\"FORBIDDEN\",\"message\":\"접근 권한이 없어요\"}")))
    })
    @GetMapping
    public AdminUsersResponse getUsers() {
        return adminUserService.getUsers();
    }
}