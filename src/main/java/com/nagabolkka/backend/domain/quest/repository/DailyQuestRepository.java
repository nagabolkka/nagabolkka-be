package com.nagabolkka.backend.domain.quest.repository;

import com.nagabolkka.backend.domain.quest.entity.DailyQuest;
import com.nagabolkka.backend.domain.quest.entity.QuestStatus;
import com.nagabolkka.backend.domain.user.entity.Role;
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

    // 모험 일지 : 기간 안 날짜별 상태 · 기분 (기록 없는 퀘스트는 mood null)
    @Query("""
            select new com.nagabolkka.backend.domain.quest.repository.DayRecord(q.questDate, q.status, r.mood)
            from DailyQuest q
            left join QuestRecord r on r.dailyQuest = q
            where q.user.id = :userId and q.questDate between :from and :to
            """)
    List<DayRecord> findDayRecords(@Param("userId") Long userId,
                                   @Param("from") LocalDate from, @Param("to") LocalDate to);

    // 관리자 : 사용자별 배정 · 완료 · 쉬기 (한 번에 GROUP BY · 하루 1개라 count = 배정된 날)
    @Query("""
            select new com.nagabolkka.backend.domain.quest.repository.QuestStats(
                q.user.id,
                count(q),
                sum(case when q.status = :completed then 1L else 0L end),
                sum(case when q.status = :skipped then 1L else 0L end))
            from DailyQuest q
            where q.user.role = :role
            group by q.user.id
            """)
    List<QuestStats> countQuestStats(@Param("role") Role role,
                                     @Param("completed") QuestStatus completed,
                                     @Param("skipped") QuestStatus skipped);

    // 편지 : 그 주 완료 수
    long countByUserIdAndStatusAndQuestDateBetween(Long userId, QuestStatus status, LocalDate from, LocalDate to);

    // 편지 : 그 주 쪼개서 완료한 수 (DailyQuest.split = is_split)
    long countByUserIdAndStatusAndSplitTrueAndQuestDateBetween(Long userId, QuestStatus status,
                                                               LocalDate from, LocalDate to);
}