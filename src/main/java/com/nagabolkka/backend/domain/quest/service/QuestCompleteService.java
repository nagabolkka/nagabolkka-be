package com.nagabolkka.backend.domain.quest.service;

import com.nagabolkka.backend.domain.quest.dto.CompleteQuestRequest;
import com.nagabolkka.backend.domain.quest.dto.CompleteQuestResponse;
import com.nagabolkka.backend.global.ai.AiClient;
import com.nagabolkka.backend.global.ai.CrisisFallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestCompleteService {

    // 조각에 복원 문구가 없을 때 (시드엔 다 있음 · 안전장치)
    private static final String DEFAULT_RESTORE_TEXT = "색이 조금 돌아왔어";

    private final QuestCompleter questCompleter;
    private final AiClient aiClient;
    private final CrisisFallback crisisFallback;

    // 트랜잭션 없음 : AI 호출 (최대 3초) 은 잠금 밖에서
    public CompleteQuestResponse complete(Long userId, CompleteQuestRequest request) {
        String reflection = request.reflectionOrNull();

        // ① 위기 검사 (회고가 있을 때만)
        boolean crisis = reflection != null && isCrisis(reflection);

        // ② 완료 처리 (트랜잭션)
        QuestCompleter.Result result = questCompleter.complete(
                userId, request.mood(), request.doneStepsOrZero(), reflection, crisis);

        if (crisis) {
            return CompleteQuestResponse.crisis(); // 연출 없이 도움 받기 화면 · 루미 한마디 안 만듦
        }

        // ③ 루미 한마디 (AI → 실패하면 템플릿)
        String restoreText = result.restoreText() == null ? DEFAULT_RESTORE_TEXT : result.restoreText();
        String lumiMessage = aiClient.lumiMessage(new AiClient.LumiRequest(
                        result.nickname(), result.missionTitle(), request.mood(), reflection,
                        result.pieceName(), restoreText))
                .map(AiClient.LumiResult::message)
                .filter(message -> !message.isBlank())
                .orElseGet(() -> LumiTemplate.of(reflection, result.pieceName(), restoreText));

        // ④ 저장 (200자 자르기는 엔티티에서)
        questCompleter.saveLumiMessage(result.recordId(), lumiMessage);

        return new CompleteQuestResponse(false, result.pieces(), lumiMessage, result.restoration(),
                result.nextDifficulty(), result.nextRegion());
    }

    private boolean isCrisis(String reflection) {
        AiClient.CrisisResult result = aiClient.checkCrisis(reflection)
                .orElseGet(() -> crisisFallback.check(reflection));
        if (result.crisis()) {
            log.info("회고 위기 감지 method={} ruleId={}", result.method(), result.ruleId()); // 원문은 남기지 않음
        }
        return result.crisis();
    }
}