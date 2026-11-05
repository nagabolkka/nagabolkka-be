package com.nagabolkka.backend.domain.user.repository;

import com.nagabolkka.backend.domain.user.entity.UserPrivateInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPrivateInfoRepository extends JpaRepository<UserPrivateInfo, Long> {

    Optional<UserPrivateInfo> findByUserId(Long userId);
}