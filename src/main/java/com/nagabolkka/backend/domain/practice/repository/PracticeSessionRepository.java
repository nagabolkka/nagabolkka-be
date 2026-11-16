package com.nagabolkka.backend.domain.practice.repository;

import com.nagabolkka.backend.domain.practice.entity.PracticeSession;
import com.nagabolkka.backend.domain.practice.entity.PracticeStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PracticeSessionRepository extends JpaRepository<PracticeSession, Long> {

    // 하루 횟수 : 오늘 시작한 세션 수 (상태 무관 · 중간에 나가도 1회)
    @Query("select count(s) from PracticeSession s where s.user.id = :userId and s.createdAt >= :from")
    long countStartedSince(@Param("userId") Long userId, @Param("from") LocalDateTime from);

    // 대화 · 끝 : 연타해도 턴이 겹치지 않도록 세션 행 잠금 (시나리오까지)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from PracticeSession s join fetch s.scenario where s.id = :id")
    Optional<PracticeSession> findByIdForUpdate(@Param("id") Long id);

    // 6번 PRACTICE_MISSION : 오늘 끝낸 연습이 이 미션을 제안했는지
    @Query("""
            select count(s) > 0 from PracticeSession s
            where s.user.id = :userId and s.status = :status
              and s.scenario.followMission.id = :missionId and s.createdAt >= :from
            """)
    boolean existsFollowMission(@Param("userId") Long userId, @Param("missionId") Long missionId,
                                @Param("status") PracticeStatus status, @Param("from") LocalDateTime from);
}