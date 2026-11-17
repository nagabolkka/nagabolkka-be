package com.nagabolkka.backend.domain.notice.entity;

import com.nagabolkka.backend.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 공지 — created_by = 등록한 관리자 (Auditing) · "오늘 하루 보지 않기" 는 프론트 localStorage
@Getter
@Entity
@Table(name = "notices")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseEntity {

    public static final int TITLE_MAX = 60;
    public static final int CONTENT_MAX = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = TITLE_MAX)
    private String title;

    @Column(nullable = false, length = CONTENT_MAX)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NoticeStatus status;

    // 등록 → 바로 게시 중
    public static Notice create(String title, String content) {
        Notice notice = new Notice();
        notice.title = title;
        notice.content = content;
        notice.status = NoticeStatus.ACTIVE;
        return notice;
    }

    // 보낸 것만 바꿈 (null = 그대로)
    public void edit(String title, String content) {
        if (title != null) {
            this.title = title;
        }
        if (content != null) {
            this.content = content;
        }
    }

    public void changeStatus(NoticeStatus status) {
        this.status = status;
    }
}