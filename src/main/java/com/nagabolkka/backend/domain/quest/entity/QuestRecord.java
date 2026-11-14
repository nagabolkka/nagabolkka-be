package com.nagabolkka.backend.domain.quest.entity;

import com.nagabolkka.backend.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

// 마음 기록 (기분 · 회고 · 루미 한마디) — 관리자 API 에서 조회하지 않음
@Getter
@Entity
@Table(name = "quest_records")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestRecord extends BaseEntity {

    public static final int LUMI_MESSAGE_MAX = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "daily_quest_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE) // 탈퇴 → daily_quests 삭제 → 같이 삭제
    private DailyQuest dailyQuest;

    // 1 힘들었어요 ~ 5 좋았어요
    @Column(nullable = false)
    private int mood;

    // 한 줄 회고 AES 암호문 (IV 12바이트 + 암호문) · 회고 없음 · 위기 감지면 null
    @Column(name = "reflection_enc", length = 1024)
    private byte[] reflectionEnc;

    // AI 한마디 or 템플릿 (트랜잭션 밖에서 만든 뒤 따로 저장)
    @Column(length = LUMI_MESSAGE_MAX)
    private String lumiMessage;

    public static QuestRecord of(DailyQuest dailyQuest, int mood, byte[] reflectionEnc) {
        QuestRecord record = new QuestRecord();
        record.dailyQuest = dailyQuest;
        record.mood = mood;
        record.reflectionEnc = reflectionEnc;
        return record;
    }

    public void saveLumiMessage(String message) {
        this.lumiMessage = message.length() > LUMI_MESSAGE_MAX
                ? message.substring(0, LUMI_MESSAGE_MAX)
                : message;
    }
}