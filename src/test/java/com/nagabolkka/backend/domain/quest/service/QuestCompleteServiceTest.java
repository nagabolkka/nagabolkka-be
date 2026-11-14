package com.nagabolkka.backend.domain.quest.service;

import com.nagabolkka.backend.domain.quest.dto.CompleteQuestRequest;
import com.nagabolkka.backend.domain.quest.dto.CompleteQuestResponse;
import com.nagabolkka.backend.domain.quest.dto.CompleteQuestResponse.PieceItem;
import com.nagabolkka.backend.domain.quest.dto.CompleteQuestResponse.Restoration;
import com.nagabolkka.backend.global.ai.AiClient;
import com.nagabolkka.backend.global.ai.CrisisFallback;
import com.nagabolkka.backend.global.common.Difficulty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class QuestCompleteServiceTest {

    private static final Long USER_ID = 1L;

    @Mock QuestCompleter completer;
    @Mock AiClient aiClient;

    QuestCompleteService service;

    @BeforeEach
    void setUp() {
        service = new QuestCompleteService(completer, aiClient, new CrisisFallback());

        given(completer.complete(eq(USER_ID), anyInt(), anyInt(), any(), anyBoolean()))
                .willReturn(new QuestCompleter.Result(10L,
                        List.of(new PieceItem(1L, "햇살 조각", 1, true)),
                        new Restoration(1, "시작의 방", 0, 20),
                        Difficulty.EASY, null,
                        "윤지", "창문 열고 바깥 공기 3분 마시기", "햇살 조각", "하늘에 색이 돌아왔어"));
    }

    @Test
    @DisplayName("회고 없음 → 위기 검사 안 함 · AI 한마디 저장 · 응답")
    void noReflection_aiMessage() {
        given(aiClient.lumiMessage(any())).willReturn(Optional.of(new AiClient.LumiResult("고마워!", false)));

        CompleteQuestResponse response = service.complete(USER_ID, new CompleteQuestRequest(4, null, 0));

        then(aiClient).should(never()).checkCrisis(any());
        then(completer).should().complete(USER_ID, 4, 0, null, false);
        then(completer).should().saveLumiMessage(10L, "고마워!");
        assertThat(response.crisisDetected()).isFalse();
        assertThat(response.lumiMessage()).isEqualTo("고마워!");
        assertThat(response.restoration().after()).isEqualTo(20);
    }

    @Test
    @DisplayName("AI 한마디 실패 → 템플릿 문장")
    void aiFails_template() {
        given(aiClient.checkCrisis(any())).willReturn(Optional.of(new AiClient.CrisisResult(false, null, null, null)));
        given(aiClient.lumiMessage(any())).willReturn(Optional.empty());

        CompleteQuestResponse response = service.complete(USER_ID, new CompleteQuestRequest(4, "바람이 시원했다", 0));

        assertThat(response.lumiMessage()).startsWith("\"바람이 시원했다\" ㅡ 그 말 기억할게.");
    }

    @Test
    @DisplayName("위기 감지 → crisis 로 완료 처리 · 루미 한마디 안 만듦 · crisisDetected 만")
    void crisis() {
        given(aiClient.checkCrisis(any())).willReturn(Optional.of(new AiClient.CrisisResult(true, "RULE", "H01", null)));

        CompleteQuestResponse response = service.complete(USER_ID, new CompleteQuestRequest(2, "죽고 싶어", 0));

        then(completer).should().complete(USER_ID, 2, 0, "죽고 싶어", true);
        then(aiClient).should(never()).lumiMessage(any());
        then(completer).should(never()).saveLumiMessage(anyLong(), any());
        assertThat(response.crisisDetected()).isTrue();
        assertThat(response.pieces()).isNull();
    }

    @Test
    @DisplayName("AI 서버가 응답 없어도 Spring 규칙으로 위기 감지")
    void aiDown_fallbackDetects() {
        given(aiClient.checkCrisis(any())).willReturn(Optional.empty());

        CompleteQuestResponse response = service.complete(USER_ID, new CompleteQuestRequest(2, "살기 싫어", 0));

        assertThat(response.crisisDetected()).isTrue();
    }

    @Test
    @DisplayName("공백만 쓴 회고 = 회고 없음")
    void blankReflection() {
        given(aiClient.lumiMessage(any())).willReturn(Optional.empty());

        service.complete(USER_ID, new CompleteQuestRequest(3, "   ", 0));

        then(aiClient).should(never()).checkCrisis(any());
        then(completer).should().complete(USER_ID, 3, 0, null, false);
    }
}