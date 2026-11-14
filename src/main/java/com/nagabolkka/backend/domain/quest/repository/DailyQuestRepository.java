package com.nagabolkka.backend.domain.quest.repository;

import com.nagabolkka.backend.domain.quest.entity.DailyQuest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.time.LocalDate;
import java.util.Optional;

public interface DailyQuestRepository extends JpaRepository<DailyQuest, Long> {

    // 오늘 퀘스트 (미션 · 지역 · 보상 조각까지 한 번에)
    @Query("""
            select q from DailyQuest q
            join fetch q.mission m
            join fetch m.rewardPiece
            join fetch q.region
            where q.user.id = :userId and q.questDate = :questDate
            """)
    Optional<DailyQuest> findToday(@Param("userId") Long userId, @Param("questDate") LocalDate questDate);

    // 배정 다양화 : 오늘 전 퀘스트의 미션 id (최근 것부터 · 개수는 Pageable 로)
    @Query("""
            select q.mission.id from DailyQuest q
            where q.user.id = :userId and q.questDate < :today
            order by q.questDate desc
            """)
    List<Long> findRecentMissionIds(@Param("userId") Long userId, @Param("today") LocalDate today, Pageable pageable);
}