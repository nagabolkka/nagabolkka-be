package com.nagabolkka.backend.domain.notice.repository;

import com.nagabolkka.backend.domain.notice.entity.Notice;
import com.nagabolkka.backend.domain.notice.entity.NoticeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // 게시 중 가장 최근 1개 (같은 시각이면 나중에 등록한 것)
    Optional<Notice> findFirstByStatusOrderByCreatedAtDescIdDesc(NoticeStatus status);

    // 전체 최근 순
    List<Notice> findAllByOrderByCreatedAtDescIdDesc();
}