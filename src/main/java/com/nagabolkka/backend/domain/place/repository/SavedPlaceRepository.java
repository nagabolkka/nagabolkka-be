package com.nagabolkka.backend.domain.place.repository;

import com.nagabolkka.backend.domain.place.entity.SavedPlace;
import com.nagabolkka.backend.domain.place.entity.SavedStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedPlaceRepository extends JpaRepository<SavedPlace, Long> {

    // 기다리는 담기
    List<SavedPlace> findByUserIdAndStatus(Long userId, SavedStatus status);

    // 배정 : 가장 최근에 담은 대기 1개
    Optional<SavedPlace> findFirstByUserIdAndStatusOrderByIdDesc(Long userId, SavedStatus status);
}