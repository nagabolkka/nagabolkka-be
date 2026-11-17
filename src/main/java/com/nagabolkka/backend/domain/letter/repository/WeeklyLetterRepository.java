package com.nagabolkka.backend.domain.letter.repository;

import com.nagabolkka.backend.domain.letter.entity.WeeklyLetter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface WeeklyLetterRepository extends JpaRepository<WeeklyLetter, Long> {

    // 배치 : 이 주 편지가 이미 있으면 건너뜀
    boolean existsByUserIdAndWeekStart(Long userId, LocalDate weekStart);

    // 13 : 가장 최근 편지
    Optional<WeeklyLetter> findFirstByUserIdOrderByWeekStartDesc(Long userId);
}