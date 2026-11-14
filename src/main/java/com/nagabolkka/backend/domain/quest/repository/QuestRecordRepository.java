package com.nagabolkka.backend.domain.quest.repository;

import com.nagabolkka.backend.domain.quest.entity.QuestRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestRecordRepository extends JpaRepository<QuestRecord, Long> {
}