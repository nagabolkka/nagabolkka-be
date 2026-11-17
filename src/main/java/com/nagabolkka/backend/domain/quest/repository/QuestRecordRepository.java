package com.nagabolkka.backend.domain.quest.repository;

import com.nagabolkka.backend.domain.quest.entity.QuestRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface QuestRecordRepository extends JpaRepository<QuestRecord, Long> {

    // 편지 : 그 주 기분 평균 (기록 없으면 null) — 회고 문장은 안 읽음
    @Query("""
            select avg(r.mood) from QuestRecord r
            where r.dailyQuest.user.id = :userId and r.dailyQuest.questDate between :from and :to
            """)
    Double averageMood(@Param("userId") Long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}