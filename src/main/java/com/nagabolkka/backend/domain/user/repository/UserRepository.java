package com.nagabolkka.backend.domain.user.repository;

import com.nagabolkka.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 로그인 : kakao_id_hash 로 사용자 조회
    @Query("select p.user from UserPrivateInfo p where p.kakaoIdHash = :kakaoIdHash")
    Optional<User> findByKakaoIdHash(@Param("kakaoIdHash") String kakaoIdHash);
}