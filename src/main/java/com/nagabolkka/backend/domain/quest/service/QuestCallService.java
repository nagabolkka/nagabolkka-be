package com.nagabolkka.backend.domain.quest.service;

import com.nagabolkka.backend.domain.quest.dto.QuestCallResponse;
import com.nagabolkka.backend.domain.quest.entity.DailyQuest;
import com.nagabolkka.backend.domain.quest.repository.DailyQuestRepository;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.ai.AiClient;
import com.nagabolkka.backend.global.ai.AiClient.LiveSessionRequest;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;

// 루미 동행 통화 : 오늘 퀘스트 정보로 AI 서버에 Realtime 임시 키 요청
// 음성 · 대화는 브라우저 ↔ OpenAI 직접 (우리 서버 · DB 에 남지 않음)
@Service
@RequiredArgsConstructor
public class QuestCallService {

    static final int MAX_SECONDS = 600; // 한 번 통화 최대 10분 (앱이 끊음)
    static final int DESCRIPTION_MAX = 300; // AI 서버 요청 제한

    private final UserRepository userRepository;
    private final DailyQuestRepository dailyQuestRepository;
    private final AiClient aiClient;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public QuestCallResponse start(Long userId) {
        // ① 오늘 퀘스트 읽기는 짧은 트랜잭션 안에서 (운영은 open-in-view false → 장소 설명 지연 로딩)
        LiveSessionRequest request = transactionTemplate.execute(status -> toRequest(userId));
        // ② AI 서버 · OpenAI 는 트랜잭션 밖에서 (기다리는 동안 DB 커넥션을 잡지 않음)
        return aiClient.liveSession(request)
                .map(result -> QuestCallResponse.of(result, MAX_SECONDS))
                .orElseThrow(() -> new CustomException(ErrorCode.CALL_UNAVAILABLE));
    }

    private LiveSessionRequest toRequest(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        if (!user.isOnboardingCompleted()) {
            throw new CustomException(ErrorCode.ONBOARDING_REQUIRED);
        }
        DailyQuest quest = dailyQuestRepository.findToday(userId, LocalDate.now(clock))
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND)); // 오늘 퀘스트를 아직 안 받음
        if (quest.isDone()) {
            throw new CustomException(ErrorCode.QUEST_ALREADY_DONE);
        }
        // 루미가 미션에 맞게 돕도록 제목 · 설명 · 목적지 (기분 · 회고 등 마음 기록은 보내지 않음)
        return new LiveSessionRequest(user.getNickname(), quest.getTitle(),
                cut(quest.getDescription(), DESCRIPTION_MAX), quest.getPlaceName());
    }

    private static String cut(String text, int max) {
        if (text == null || text.length() <= max) {
            return text;
        }
        return text.substring(0, max);
    }
}