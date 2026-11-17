package com.nagabolkka.backend.domain.user.repository;

import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.entity.Role;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.time.LocalDateTime;

public interface UserRepository extends JpaRepository<User, Long> {

    // 로그인 : kakao_id_hash 로 사용자 조회
    @Query("select p.user from UserPrivateInfo p where p.kakaoIdHash = :kakaoIdHash")
    Optional<User> findByKakaoIdHash(@Param("kakaoIdHash") String kakaoIdHash);

    // 시작 체크 : "모험 시작하기" 를 두 번 눌러도 하나만 통과하도록 사용자 행을 잠근다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    // 관리자 사용자 현황 : 역할별 (현재 계단까지 한 번에) · id 순
    @Query("select u from User u left join fetch u.currentRegion where u.role = :role order by u.id")
    List<User> findAllWithRegionByRole(@Param("role") Role role);

    // 편지 대상 : 시작 체크를 마친 역할별 사용자 · before 전에 가입 (현재 지역 · 구까지 한 번에)
    @Query("""
            select u from User u
            left join fetch u.currentRegion
            left join fetch u.district
            where u.role = :role and u.startScore is not null and u.createdAt < :before
            order by u.id
            """)
    List<User> findLetterTargets(@Param("role") Role role, @Param("before") LocalDateTime before);
}