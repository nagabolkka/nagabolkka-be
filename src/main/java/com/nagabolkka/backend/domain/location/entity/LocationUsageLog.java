package com.nagabolkka.backend.domain.location.entity;

import com.nagabolkka.backend.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "location_usage_logs", indexes = {
        @Index(columnList = "user_id, created_at"),
        @Index(columnList = "created_at")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LocationUsageLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LocationPurpose purpose;

    public static LocationUsageLog of(Long userId, LocationPurpose purpose) {
        LocationUsageLog log = new LocationUsageLog();
        log.userId = userId;
        log.purpose = purpose;
        return log;
    }
}