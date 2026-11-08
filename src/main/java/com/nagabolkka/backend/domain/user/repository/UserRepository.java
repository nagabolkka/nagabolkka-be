package com.nagabolkka.backend.domain.user.repository;

import com.nagabolkka.backend.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 로그인 : kakao_id_hash 로 사용자 조회
    @Query("select p.user from UserPrivateInfo p where p.kakaoIdHash = :kakaoIdHash")
    Optional<User> findByKakaoIdHash(@Param("kakaoIdHash") String kakaoIdHash);

    // 시작 체크 : "모험 시작하기" 를 두 번 눌러도 하나만 통과하도록 사용자 행을 잠근다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}