package com.nagabolkka.backend.domain.letter.controller;

import com.nagabolkka.backend.domain.letter.batch.WeeklyLetterJob;
import com.nagabolkka.backend.domain.letter.service.WeeklyLetterGenerator;
import com.nagabolkka.backend.domain.letter.service.WeeklyLetterGenerator.Result;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;

@Tag(name = "관리자 ㅡ 편지", description = "주간 편지 수동 생성 (ADMIN)")
@RestController
@RequestMapping("/api/admin/letters")
@RequiredArgsConstructor
public class AdminLetterController {

    private final WeeklyLetterGenerator generator;
    private final Clock clock;

    @Operation(summary = "주간 편지 지금 만들기",
            description = "월요일 10:00 배치를 지금 실행합니다. 이미 그 주 편지가 있는 사용자는 건너뜁니다.\n\n"
                    + "- `weekStart` 없음 → 지난주\n"
                    + "- `weekStart` = 그 주 **월요일** (yyyy-MM-dd) 이번 주로 미리 만들어 보기 · 배치 실패 후 다시 만들기에 사용\n"
                    + "- 사용자마다 AI 를 부르므로 사용자가 많으면 오래 걸릴 수 있습니다.")
    @PostMapping("/generate")
    public Result generate(@Parameter(description = "주 시작 월요일 (선택)", example = "2026-11-09")
                           @RequestParam(required = false) LocalDate weekStart) {
        LocalDate target = weekStart == null ? WeeklyLetterJob.lastWeekStart(LocalDate.now(clock)) : weekStart;
        if (target.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        return generator.generate(target);
    }
}