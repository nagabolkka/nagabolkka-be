package com.nagabolkka.backend.domain.user.repository;

import com.nagabolkka.backend.domain.user.entity.UserPrivateInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserPrivateInfoRepository extends JpaRepository<UserPrivateInfo, Long> {

    Optional<UserPrivateInfo> findByUserId(Long userId);

    // 탈퇴 : 카카오 회원번호 암호문만 (엔티티를 불러오지 않음 → 사용자 삭제 때 Hibernate 가 꼬이지 않음)
    @Query("select p.kakaoIdEnc from UserPrivateInfo p where p.user.id = :userId")
    Optional<byte[]> findKakaoIdEncByUserId(@Param("userId") Long userId);
}