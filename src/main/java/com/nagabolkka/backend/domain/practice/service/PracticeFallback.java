package com.nagabolkka.backend.domain.practice.service;

import com.nagabolkka.backend.global.ai.AiClient.PracticeFeedback;
import com.nagabolkka.backend.global.ai.AiClient.PracticeOpening;
import com.nagabolkka.backend.global.ai.AiClient.PracticeReply;

import java.util.List;
import java.util.Map;

// AI 서버가 꺼졌을 때 대체값 (OpenAI 실패는 AI 서버가 먼저 대체함)
// 첫 대사 · 피드백 = AI 서버 scenarios/*.yaml 의 opening · choices · fallback_feedback 복사 (바꾸면 같이)
public final class PracticeFallback {

    private record Script(String opening, List<String> choices, List<String> good, List<String> next) {
    }

    private static final Map<String, Script> SCRIPTS = Map.of(
            "FIRST_CALL", new Script(
                    "네, OO도서관입니다. 무엇을 도와드릴까요?",
                    List.of("북토크 문의하려고 전화했어요.", "혹시 이번 달 프로그램 신청 되나요?"),
                    List.of("끝까지 전화를 이어갔어요", "필요한 걸 직접 물어봤어요"),
                    List.of("몇 명까지 신청할 수 있나요?")),
            "FIRST_VISIT", new Script(
                    "안녕하세요, 어떻게 오셨어요?",
                    List.of("회원증 만들려고 왔어요.", "저, 처음 와서요. 뭐부터 하면 될까요?"),
                    List.of("먼저 용건을 말했어요", "모르는 걸 직접 물어봤어요"),
                    List.of("책은 어디에서 빌리면 되나요?")),
            "INTERVIEW", new Script(
                    "안녕하세요, 편하게 앉으세요. 간단하게 자기소개 한번 해 주시겠어요?",
                    List.of("안녕하세요, 이 동네에 살고 있어서 지원했습니다.",
                            "아직 경험은 없지만 열심히 배우고 싶어서 지원했습니다."),
                    List.of("질문에 끝까지 대답했어요", "정중하게 인사로 마무리했어요"),
                    List.of("처음에 일은 어떻게 배우나요?")));

    private static final Script DEFAULT = new Script(
            "안녕하세요, 무엇을 도와드릴까요?",
            List.of("안녕하세요.", "여쭤볼 게 있어서요."),
            List.of("끝까지 대화를 이어갔어요"),
            List.of("한 가지만 더 여쭤봐도 될까요?"));

    // 답변은 시나리오 공통 (대화 흐름을 몰라도 어색하지 않은 말)
    static final String REPLY = "네, 말씀 잘 들었어요. 조금 더 이야기해 주시겠어요?";
    static final List<String> REPLY_CHOICES = List.of("네, 더 말씀드릴게요.", "궁금한 게 하나 있어요.");
    static final String CLOSING = "오늘 이야기 나눠 주셔서 고마워요. 좋은 하루 보내세요!";

    private PracticeFallback() {
    }

    public static PracticeOpening opening(String scenarioCode) {
        Script script = SCRIPTS.getOrDefault(scenarioCode, DEFAULT);
        return new PracticeOpening(script.opening(), script.choices());
    }

    // 마지막 턴 → 마무리 인사 · 선택지 없음
    public static PracticeReply reply(boolean lastTurn) {
        return lastTurn
                ? new PracticeReply(CLOSING, List.of(), true, true)
                : new PracticeReply(REPLY, REPLY_CHOICES, false, true);
    }

    public static PracticeFeedback feedback(String scenarioCode) {
        Script script = SCRIPTS.getOrDefault(scenarioCode, DEFAULT);
        return new PracticeFeedback(script.good(), script.next());
    }
}