package com.nagabolkka.backend.global.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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

    // AI 서버 schemas.py 와 같은 모양
    public record CrisisResult(boolean crisis, String method, String ruleId, String category) {
    }

    public record LumiRequest(String nickname, String missionTitle, int mood, String reflection,
                              String pieceName, String restoreText) {
    }

    public record LumiResult(String message, boolean fallback) {
    }

    // 회고 위기 검사
    public Optional<CrisisResult> checkCrisis(String text) {
        return call(() -> post("/ai/crisis/check", Map.of("text", text), CrisisResult.class));
    }

    // 루미 한마디
    public Optional<LumiResult> lumiMessage(LumiRequest request) {
        return call(() -> post("/ai/lumi/message", request, LumiResult.class));
    }

    private <T> T post(String uri, Object body, Class<T> type) {
        return aiRestClient.post().uri(uri).body(body).retrieve().body(type);
    }

    private <T> Optional<T> call(Supplier<T> request) {
        try {
            return Optional.ofNullable(request.get());
        } catch (RestClientException e) {
            // 원문(회고)은 로그에 남기지 않는다 · 원인 예외 이름만 (응답 시간 초과 / 연결 실패 구분)
            log.warn("AI 서버 호출 실패: {} ({})", e.getClass().getSimpleName(),
                    e.getMostSpecificCause().getClass().getSimpleName());
            return Optional.empty();
        }
    }
}