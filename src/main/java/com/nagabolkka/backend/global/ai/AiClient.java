package com.nagabolkka.backend.global.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Component
public class AiClient {

    private final RestClient aiRestClient;

    public AiClient(RestClient aiRestClient) {
        this.aiRestClient = aiRestClient;
    }

    public record CrisisResult(boolean crisis, String method, String ruleId, String category) {
    }

    public record LumiRequest(String nickname, String missionTitle, int mood, String reflection,
                              String pieceName, String restoreText) {
    }

    public record LumiResult(String message, boolean fallback) {
    }

    // 연습 대화 한 줄 (role = user · partner)
    public record PracticeLine(String role, String text) {
    }

    public record PracticeOpening(String opening, List<String> choices) {
    }

    public record PracticeReplyRequest(String scenarioCode, int turn, int maxTurns, String message,
                                       List<PracticeLine> history) {
    }

    public record PracticeReply(String reply, List<String> choices, boolean finished, boolean fallback) {
    }

    public record PracticeFeedbackRequest(String scenarioCode, List<PracticeLine> history) {
    }

    public record PracticeFeedback(List<String> good, List<String> next) {
    }

    public record PracticeFeedbackResult(PracticeFeedback feedback, boolean fallback) {
    }

    // 회고 · 연습 메시지 위기 검사
    public Optional<CrisisResult> checkCrisis(String text) {
        return call(() -> post("/ai/crisis/check", Map.of("text", text), CrisisResult.class));
    }

    // 루미 한마디
    public Optional<LumiResult> lumiMessage(LumiRequest request) {
        return call(() -> post("/ai/lumi/message", request, LumiResult.class));
    }

    // 연습 첫 대사 · 선택지 (yaml 고정값)
    public Optional<PracticeOpening> practiceOpening(String scenarioCode) {
        return call(() -> aiRestClient.get()
                .uri("/ai/practice/scenarios/{code}/opening", scenarioCode)
                .retrieve()
                .body(PracticeOpening.class));
    }

    // 연습 상대역 답변
    public Optional<PracticeReply> practiceReply(PracticeReplyRequest request) {
        return call(() -> post("/ai/practice/reply", request, PracticeReply.class));
    }

    // 연습 피드백
    public Optional<PracticeFeedback> practiceFeedback(PracticeFeedbackRequest request) {
        return call(() -> post("/ai/practice/feedback", request, PracticeFeedbackResult.class))
                .map(PracticeFeedbackResult::feedback);
    }

    private <T> T post(String uri, Object body, Class<T> type) {
        return aiRestClient.post().uri(uri).body(body).retrieve().body(type);
    }

    private <T> Optional<T> call(Supplier<T> request) {
        try {
            return Optional.ofNullable(request.get());
        } catch (RestClientException e) {
            // 원문(회고 · 연습 대화)은 로그에 남기지 않는다 · 원인 예외 이름만 (응답 시간 초과 / 연결 실패 구분)
            log.warn("AI 서버 호출 실패: {} ({})", e.getClass().getSimpleName(),
                    e.getMostSpecificCause().getClass().getSimpleName());
            return Optional.empty();
        }
    }
}