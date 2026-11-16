package com.nagabolkka.backend.domain.practice.service;

import com.nagabolkka.backend.domain.piece.entity.Piece;
import com.nagabolkka.backend.domain.piece.entity.PieceType;
import com.nagabolkka.backend.domain.practice.dto.FinishRequest;
import com.nagabolkka.backend.domain.practice.dto.FinishResponse;
import com.nagabolkka.backend.domain.practice.dto.FinishResponse.FollowMission;
import com.nagabolkka.backend.domain.practice.dto.FinishResponse.PieceItem;
import com.nagabolkka.backend.domain.practice.dto.PracticeLineRequest;
import com.nagabolkka.backend.domain.practice.dto.ScenariosResponse;
import com.nagabolkka.backend.domain.practice.dto.StartPracticeRequest;
import com.nagabolkka.backend.domain.practice.dto.StartPracticeResponse;
import com.nagabolkka.backend.domain.practice.dto.TurnRequest;
import com.nagabolkka.backend.domain.practice.dto.TurnResponse;
import com.nagabolkka.backend.domain.practice.entity.PracticeScenario;
import com.nagabolkka.backend.domain.practice.repository.PracticeScenarioRepository;
import com.nagabolkka.backend.domain.practice.service.PracticeSessionManager.Finished;
import com.nagabolkka.backend.domain.practice.service.PracticeSessionManager.Started;
import com.nagabolkka.backend.domain.practice.service.PracticeSessionManager.Turn;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.global.ai.AiClient;
import com.nagabolkka.backend.global.ai.AiClient.CrisisResult;
import com.nagabolkka.backend.global.ai.AiClient.PracticeReply;
import com.nagabolkka.backend.global.ai.AiClient.PracticeReplyRequest;
import com.nagabolkka.backend.global.ai.CrisisFallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PracticeServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long SESSION_ID = 55L;
    private static final List<PracticeLineRequest> HISTORY =
            List.of(new PracticeLineRequest("partner", "네, OO도서관입니다. 무엇을 도와드릴까요?"));
    private static final CrisisResult PASS = new CrisisResult(false, null, null, null);

    @Mock PracticeScenarioRepository scenarioRepository;
    @Mock PracticeSessionManager sessionManager;
    @Mock AiClient aiClient;
    @Mock CrisisFallback crisisFallback;

    PracticeService service;

    @BeforeEach
    void setUp() {
        service = new PracticeService(scenarioRepository, sessionManager, aiClient, crisisFallback,
                JsonMapper.builder().build());
    }

    @Test
    @DisplayName("목록 : 남은 횟수 · 보상 조각 이름 · 계단")
    void getScenarios() {
        Region stage5 = Region.of(5, "새로운 세상", "사회 연결", "희망의 조각", "#BFB2F2", "a.png", "b.png");
        Piece confidence = Piece.of(stage5, 4, "자신감 조각", PieceType.PRACTICE, "복원", "힌트");
        PracticeScenario scenario = PracticeScenario.of("FIRST_CALL", "첫 전화", "설명", "도서관 안내 직원",
                3, false, confidence, null);
        given(scenarioRepository.findAllByOrderByIdAsc()).willReturn(List.of(scenario));
        given(sessionManager.remainingToday(USER_ID)).willReturn(2);

        ScenariosResponse res = service.getScenarios(USER_ID);

        assertThat(res.remainingToday()).isEqualTo(2);
        assertThat(res.scenarios()).singleElement().satisfies(item -> {
            assertThat(item.code()).isEqualTo("FIRST_CALL");
            assertThat(item.rewardPieceName()).isEqualTo("자신감 조각");
            assertThat(item.rewardStageNo()).isEqualTo(5);
            assertThat(item.isPlus()).isFalse();
        });
    }

    @Test
    @DisplayName("시작 · AI 서버 꺼짐 → 시나리오 첫 대사 대체값")
    void start_aiDown() {
        given(sessionManager.start(USER_ID, 1L)).willReturn(new Started(SESSION_ID, "FIRST_CALL", 3));
        given(aiClient.practiceOpening("FIRST_CALL")).willReturn(Optional.empty());

        StartPracticeResponse res = service.start(USER_ID, new StartPracticeRequest(1L));

        assertThat(res.sessionId()).isEqualTo(SESSION_ID);
        assertThat(res.opening()).isEqualTo("네, OO도서관입니다. 무엇을 도와드릴까요?");
        assertThat(res.choices()).hasSize(2);
        assertThat(res.maxTurns()).isEqualTo(3);
    }

    @Test
    @DisplayName("대화 : 앞뒤 공백 제거 · 서버 턴 번호 · AI 답변 · 선택지")
    void turn() {
        given(sessionManager.reserveTurn(USER_ID, SESSION_ID)).willReturn(new Turn("FIRST_CALL", 1, 3));
        given(aiClient.checkCrisis("북토크 문의하려고 전화했어요.")).willReturn(Optional.of(PASS));
        given(aiClient.practiceReply(any())).willReturn(Optional.of(
                new PracticeReply("아 토요 북토크요!", List.of("네, 신청할게요.", "더 알려주세요."), false, false)));

        TurnResponse res = service.turn(USER_ID, SESSION_ID, new TurnRequest("  북토크 문의하려고 전화했어요.  ", HISTORY));

        assertThat(res.crisisDetected()).isFalse();
        assertThat(res.reply()).isEqualTo("아 토요 북토크요!");
        assertThat(res.turn()).isEqualTo(1);
        assertThat(res.choices()).hasSize(2);
        assertThat(res.finished()).isFalse();

        ArgumentCaptor<PracticeReplyRequest> captor = ArgumentCaptor.forClass(PracticeReplyRequest.class);
        then(aiClient).should().practiceReply(captor.capture());
        assertThat(captor.getValue().message()).isEqualTo("북토크 문의하려고 전화했어요.");
        assertThat(captor.getValue().turn()).isEqualTo(1);
        assertThat(captor.getValue().history()).hasSize(1);
    }

    @Test
    @DisplayName("대화 · 위기 (AI 서버 꺼짐 → BE 자체 규칙) → 세션 멈춤 · AI 답변 안 부름")
    void turn_crisis() {
        String message = "오늘 그냥 죽고 싶었어";
        given(sessionManager.reserveTurn(USER_ID, SESSION_ID)).willReturn(new Turn("FIRST_CALL", 2, 3));
        given(aiClient.checkCrisis(message)).willReturn(Optional.empty());
        given(crisisFallback.check(message)).willReturn(new CrisisResult(true, "RULE", "H01", null));

        TurnResponse res = service.turn(USER_ID, SESSION_ID, new TurnRequest(message, HISTORY));

        assertThat(res.crisisDetected()).isTrue();
        assertThat(res.reply()).isNull();
        then(sessionManager).should().stopForCrisis(USER_ID, SESSION_ID);
        then(aiClient).should(never()).practiceReply(any());
    }

    @Test
    @DisplayName("마지막 턴 → finished true · 선택지 비움 (AI 가 선택지를 줘도)")
    void turn_last() {
        given(sessionManager.reserveTurn(USER_ID, SESSION_ID)).willReturn(new Turn("FIRST_CALL", 3, 3));
        given(aiClient.checkCrisis("감사합니다.")).willReturn(Optional.of(PASS));
        given(aiClient.practiceReply(any())).willReturn(Optional.of(
                new PracticeReply("토요일에 뵐게요.", List.of("a", "b"), false, false)));

        TurnResponse res = service.turn(USER_ID, SESSION_ID, new TurnRequest("감사합니다.", HISTORY));

        assertThat(res.finished()).isTrue();
        assertThat(res.choices()).isEmpty();
    }

    @Test
    @DisplayName("대화 · AI 서버 꺼짐 → 공통 고정 대사")
    void turn_aiDown() {
        given(sessionManager.reserveTurn(USER_ID, SESSION_ID)).willReturn(new Turn("FIRST_CALL", 2, 3));
        given(aiClient.checkCrisis("네, 신청할게요.")).willReturn(Optional.of(PASS));
        given(aiClient.practiceReply(any())).willReturn(Optional.empty());

        TurnResponse res = service.turn(USER_ID, SESSION_ID, new TurnRequest("네, 신청할게요.", HISTORY));

        assertThat(res.reply()).isEqualTo(PracticeFallback.REPLY);
        assertThat(res.choices()).isEqualTo(PracticeFallback.REPLY_CHOICES);
        assertThat(res.finished()).isFalse();
    }

    @Test
    @DisplayName("끝 · AI 서버 꺼짐 → 시나리오 고정 피드백 · JSON 저장 · 조각 · 실제 미션")
    void finish_aiDown() {
        given(sessionManager.complete(USER_ID, SESSION_ID)).willReturn(new Finished("FIRST_VISIT",
                new PieceItem(20L, "연습 조각", 4, "만남의 정원", true),
                new FollowMission(78L, "도서관에 가서 회원증 만들기")));
        given(aiClient.practiceFeedback(any())).willReturn(Optional.empty());

        FinishResponse res = service.finish(USER_ID, SESSION_ID, new FinishRequest(HISTORY));

        assertThat(res.feedback().good()).containsExactly("먼저 용건을 말했어요", "모르는 걸 직접 물어봤어요");
        assertThat(res.feedback().next()).containsExactly("책은 어디에서 빌리면 되나요?");
        assertThat(res.piece().name()).isEqualTo("연습 조각");
        assertThat(res.followMission().missionId()).isEqualTo(78L);

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        then(sessionManager).should().saveFeedback(eq(SESSION_ID), json.capture());
        assertThat(json.getValue()).contains("\"good\"").contains("먼저 용건을 말했어요");
    }
}