package com.nagabolkka.backend.domain.push.repository;

import com.nagabolkka.backend.domain.push.entity.FcmToken;
import com.nagabolkka.backend.domain.quest.entity.QuestStatus;
import com.nagabolkka.backend.domain.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    Optional<FcmToken> findByToken(String token);

    @Modifying
    @Transactional
    @Query("delete from FcmToken t where t.token in :tokens")
    void deleteAllByTokenIn(@Param("tokens") Collection<String> tokens);

    @Query("select t.token from FcmToken t where t.user.id in :userIds")
    List<String> findTokensByUserIds(@Param("userIds") Collection<Long> userIds);

    // 오늘의 퀘스트 알림 대상 : 일반 사용자 · 시작 체크 완료 · 오늘 이미 완료 · 쉬기 아님
    @Query("""
            select t.token from FcmToken t join t.user u
            where u.role = :role and u.startScore is not null
              and not exists (select q.id from DailyQuest q
                              where q.user = u and q.questDate = :today and q.status in :done)
            """)
    List<String> findQuestTargetTokens(@Param("role") Role role, @Param("today") LocalDate today,
                                       @Param("done") Collection<QuestStatus> done);

    // 점검 알림 대상 (12번) : 알림 켠 일반 사용자 전부
    @Query("select t.token from FcmToken t where t.user.role = :role")
    List<String> findTokensByRole(@Param("role") Role role);
}