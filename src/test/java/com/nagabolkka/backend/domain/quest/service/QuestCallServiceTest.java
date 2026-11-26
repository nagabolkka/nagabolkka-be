package com.nagabolkka.backend.domain.quest.service;

import com.nagabolkka.backend.domain.quest.dto.QuestCallResponse;
import com.nagabolkka.backend.domain.quest.entity.DailyQuest;
import com.nagabolkka.backend.domain.quest.repository.DailyQuestRepository;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.ai.AiClient;
import com.nagabolkka.backend.global.ai.AiClient.LiveSessionRequest;
import com.nagabolkka.backend.global.ai.AiClient.LiveSessionResult;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class QuestCallServiceTest {

    private static final Long USER_ID = 1L;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-26T01:00:00Z"), KST); // 10:00 KST
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 26);

    @Mock UserRepository userRepository;
    @Mock DailyQuestRepository dailyQuestRepository;
    @Mock AiClient aiClient;

    QuestCallService service;
    User user;
    DailyQuest quest;

    @BeforeEach
    void setUp() {
        // 트랜잭션 없이 바로 실행하는 템플릿 (단위 테스트)
        TransactionTemplate tx = new TransactionTemplate() {
            @Override
            public <T> T execute(TransactionCallback<T> action) {
                return action.doInTransaction(null);
            }
        };
        service = new QuestCallService(userRepository, dailyQuestRepository, aiClient, CLOCK, tx);
        user = mock(User.class);
        quest = mock(DailyQuest.class);
    }

    private void givenUser(boolean onboarded) {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(user.isOnboardingCompleted()).willReturn(onboarded);
    }

    @Test
    @DisplayName("오늘 퀘스트 제목 · 목적지 · 닉네임으로 임시 키를 받아 최대 10분과 함께 돌려줌")
    void start() {
        givenUser(true);
        given(user.getNickname()).willReturn("윤지");
        given(dailyQuestRepository.findToday(USER_ID, TODAY)).willReturn(Optional.of(quest));
        given(quest.isDone()).willReturn(false);
        given(quest.getTitle()).willReturn("집 앞 놀이터 벤치까지 10분 걷기");
        given(quest.getDescription()).willReturn("천천히 걸어가 벤치에 앉아 봐요.");
        given(quest.getPlaceName()).willReturn("놀이터 벤치");
        given(aiClient.liveSession(new LiveSessionRequest("윤지", "집 앞 놀이터 벤치까지 10분 걷기",
                "천천히 걸어가 벤치에 앉아 봐요.", "놀이터 벤치")))
                .willReturn(Optional.of(new LiveSessionResult("ek_test", 1_795_700_000L, "gpt-realtime-2.1-mini")));

        QuestCallResponse response = service.start(USER_ID);

        assertThat(response.clientSecret()).isEqualTo("ek_test");
        assertThat(response.expiresAt()).isEqualTo(1_795_700_000L);
        assertThat(response.model()).isEqualTo("gpt-realtime-2.1-mini");
        assertThat(response.maxSeconds()).isEqualTo(600);
    }

    @Test
    @DisplayName("AI 서버 · OpenAI 실패 → 409 CALL_UNAVAILABLE")
    void aiFail() {
        givenUser(true);
        given(user.getNickname()).willReturn("윤지");
        given(dailyQuestRepository.findToday(USER_ID, TODAY)).willReturn(Optional.of(quest));
        given(quest.isDone()).willReturn(false);
        given(quest.getTitle()).willReturn("창문 열기");
        given(aiClient.liveSession(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.start(USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CALL_UNAVAILABLE);
    }

    @Test
    @DisplayName("이미 끝난 퀘스트 → 409 QUEST_ALREADY_DONE (AI 호출 안 함)")
    void questDone() {
        givenUser(true);
        given(dailyQuestRepository.findToday(USER_ID, TODAY)).willReturn(Optional.of(quest));
        given(quest.isDone()).willReturn(true);

        assertThatThrownBy(() -> service.start(USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.QUEST_ALREADY_DONE);
        then(aiClient).should(never()).liveSession(any());
    }

    @Test
    @DisplayName("오늘 퀘스트 없음 → 404 NOT_FOUND")
    void noQuest() {
        givenUser(true);
        given(dailyQuestRepository.findToday(USER_ID, TODAY)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.start(USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("시작 체크 전 → 409 ONBOARDING_REQUIRED")
    void beforeOnboarding() {
        givenUser(false);

        assertThatThrownBy(() -> service.start(USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ONBOARDING_REQUIRED);
        then(dailyQuestRepository).shouldHaveNoInteractions();
    }
}