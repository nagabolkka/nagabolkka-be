package com.nagabolkka.backend.domain.letter.batch;

import com.nagabolkka.backend.domain.letter.service.WeeklyLetterGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class WeeklyLetterJob {

    private final WeeklyLetterGenerator generator;
    private final Clock clock;

    // 매주 월 03:00 KST → 지난주 (월 ~ 일) 편지
    @Scheduled(cron = "${letter.weekly-cron:0 0 3 * * MON}", zone = "Asia/Seoul")
    public void run() {
        generator.generate(lastWeekStart(LocalDate.now(clock)));
    }

    public static LocalDate lastWeekStart(LocalDate today) {
        return today.with(DayOfWeek.MONDAY).minusWeeks(1);
    }
}