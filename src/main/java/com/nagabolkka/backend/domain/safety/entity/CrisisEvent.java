package com.nagabolkka.backend.domain.safety.entity;

import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

// 위기 감지 기록 — 문장 · 내용은 저장 금지, 언제 · 어디서만 (created_at = 감지 시각)
@Getter
@Entity
@Table(name = "crisis_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrisisEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CrisisSource source;

    public static CrisisEvent of(User user, CrisisSource source) {
        CrisisEvent event = new CrisisEvent();
        event.user = user;
        event.source = source;
        return event;
    }
}