package com.nagabolkka.backend.domain.practice.service;

import com.nagabolkka.backend.domain.practice.dto.FinishRequest;
import com.nagabolkka.backend.domain.practice.dto.FinishResponse;
import com.nagabolkka.backend.domain.practice.dto.PracticeLineRequest;
import com.nagabolkka.backend.domain.practice.dto.ScenariosResponse;
import com.nagabolkka.backend.domain.practice.dto.ScenariosResponse.ScenarioItem;
import com.nagabolkka.backend.domain.practice.dto.StartPracticeRequest;
import com.nagabolkka.backend.domain.practice.dto.StartPracticeResponse;
import com.nagabolkka.backend.domain.practice.dto.TurnRequest;
import com.nagabolkka.backend.domain.practice.dto.TurnResponse;
import com.nagabolkka.backend.domain.practice.repository.PracticeScenarioRepository;
import com.nagabolkka.backend.domain.practice.service.PracticeSessionManager.Finished;
import com.nagabolkka.backend.domain.practice.service.PracticeSessionManager.Started;
import com.nagabolkka.backend.domain.practice.service.PracticeSessionManager.Turn;
import com.nagabolkka.backend.global.ai.AiClient;
import com.nagabolkka.backend.global.ai.AiClient.PracticeFeedback;
import com.nagabolkka.backend.global.ai.AiClient.PracticeFeedbackRequest;
import com.nagabolkka.backend.global.ai.AiClient.PracticeLine;
import com.nagabolkka.backend.global.ai.AiClient.PracticeOpening;
import com.nagabolkka.backend.global.ai.AiClient.PracticeReply;
import com.nagabolkka.backend.global.ai.AiClient.PracticeReplyRequest;
import com.nagabolkka.backend.global.ai.CrisisFallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PracticeService {

    private final PracticeScenarioRepository scenarioRepository;
    private final PracticeSessionManager sessionManager;
    private final AiClient aiClient;
    private final CrisisFallback crisisFallback;
    private final JsonMapper jsonMapper;

    // 14 시나리오 + 오늘 남은 횟수
    @Transactional(readOnly = true)
    public ScenariosResponse getScenarios(Long userId) {
        List<ScenarioItem> scenarios = scenarioRepository.findAllByOrderByIdAsc().stream()
                .map(ScenarioItem::of)
                .toList();
        return new ScenariosResponse(sessionManager.remainingToday(userId), scenarios);
    }

    // 시작 → 첫 대사 (AI 서버 yaml, 꺼져 있으면 BE 복사본)
    public StartPracticeResponse start(Long userId, StartPracticeRequest request) {
        Started started = sessionManager.start(userId, request.scenarioId());
        PracticeOpening opening = aiClient.practiceOpening(started.scenarioCode())
                .filter(o -> o.opening() != null && !o.opening().isBlank())
                .orElseGet(() -> PracticeFallback.opening(started.scenarioCode()));
        return new StartPracticeResponse(started.sessionId(), opening.opening(), opening.choices(), started.maxTurns());
    }

    // 대화 : ① 턴 +1 (404 · 409 는 AI 전에) ② 위기 검사 ③ 상대역 답변
    public TurnResponse turn(Long userId, Long sessionId, TurnRequest request) {
        String message = request.message().strip();
        Turn turn = sessionManager.reserveTurn(userId, sessionId);

        if (isCrisis(message)) {
            sessionManager.stopForCrisis(userId, sessionId);
            return TurnResponse.crisis(); // 연출 없이 도움 받기 · AI 답변 안 만듦
        }

        PracticeReply reply = aiClient.practiceReply(new PracticeReplyRequest(
                        turn.scenarioCode(), turn.turn(), turn.maxTurns(), message, toLines(request.history())))
                .filter(r -> r.reply() != null && !r.reply().isBlank())
                .orElseGet(() -> PracticeFallback.reply(turn.last()));

        // 마지막 턴 판단은 서버 턴 수 기준 (AI 의 finished 는 믿지 않음)
        List<String> choices = turn.last() || reply.choices() == null ? List.of() : reply.choices();
        return TurnResponse.reply(reply.reply(), turn.turn(), choices, turn.last());
    }

    // 끝 : ① COMPLETED · 연습 조각 ② AI 피드백 ③ 피드백 저장
    public FinishResponse finish(Long userId, Long sessionId, FinishRequest request) {
        Finished finished = sessionManager.complete(userId, sessionId);

        PracticeFeedback feedback = aiClient.practiceFeedback(
                        new PracticeFeedbackRequest(finished.scenarioCode(), toLines(request.history())))
                .filter(f -> f.good() != null && !f.good().isEmpty() && f.next() != null && !f.next().isEmpty())
                .orElseGet(() -> PracticeFallback.feedback(finished.scenarioCode()));

        sessionManager.saveFeedback(sessionId, toJson(feedback));
        return new FinishResponse(new FinishResponse.FeedbackItem(feedback.good(), feedback.next()),
                finished.piece(), finished.followMission());
    }

    // AI 서버 위기 검사
    private boolean isCrisis(String message) {
        AiClient.CrisisResult result = aiClient.checkCrisis(message)
                .orElseGet(() -> crisisFallback.check(message));
        if (result.crisis()) {
            log.info("연습 위기 감지 method={} ruleId={}", result.method(), result.ruleId()); // 원문은 남기지 않음
        }
        return result.crisis();
    }

    // 저장 실패해도 응답은 그대로 (피드백 기록은 부가 정보)
    private String toJson(PracticeFeedback feedback) {
        try {
            return jsonMapper.writeValueAsString(feedback);
        } catch (JacksonException e) {
            log.warn("연습 피드백 JSON 변환 실패: {}", e.getClass().getSimpleName());
            return null;
        }
    }

    private static List<PracticeLine> toLines(List<PracticeLineRequest> history) {
        return history.stream().map(line -> new PracticeLine(line.role(), line.text())).toList();
    }
}