package com.nagabolkka.backend.domain.user.repository;

import com.nagabolkka.backend.domain.user.entity.UserConsent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserConsentRepository extends JpaRepository<UserConsent, Long> {
}