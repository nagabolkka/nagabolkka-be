package com.nagabolkka.backend.domain.push.service;

import com.nagabolkka.backend.domain.push.repository.FcmTokenRepository;
import com.nagabolkka.backend.domain.quest.entity.QuestStatus;
import com.nagabolkka.backend.domain.user.entity.Role;
import com.nagabolkka.backend.global.config.ClockConfig;
import com.nagabolkka.backend.global.push.FcmSender;
import com.nagabolkka.backend.global.push.PushMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PushNotifierTest {

    // 2026-11-21 (토) 09:00 KST
    private static final Clock CLOCK = Clock.fixed(
            ZonedDateTime.of(2026, 11, 21, 9, 0, 0, 0, ClockConfig.KST).toInstant(), ClockConfig.KST);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 21);
    private static final List<QuestStatus> DONE = List.of(QuestStatus.COMPLETED, QuestStatus.SKIPPED);

    @Mock FcmTokenRepository tokenRepository;
    @Mock FcmSender sender;

    PushNotifier notifier;

    @BeforeEach
    void setUp() {
        notifier = new PushNotifier(tokenRepository, sender, CLOCK);
    }

    @Test
    @DisplayName("오늘의 퀘스트 → 오늘 완료 · 쉬기 제외 대상에게 퀘스트 알림")
    void questArrived() {
        given(sender.enabled()).willReturn(true);
        given(tokenRepository.findQuestTargetTokens(Role.USER, TODAY, DONE)).willReturn(List.of("a", "b"));
        given(sender.send(List.of("a", "b"), PushMessage.quest())).willReturn(new FcmSender.Result(2, 0, List.of()));

        notifier.questArrived();

        then(sender).should().send(List.of("a", "b"), PushMessage.quest());
        then(tokenRepository).should(never()).deleteAllByTokenIn(any());
    }

    @Test
    @DisplayName("만료 토큰 → 그 토큰만 삭제")
    void questArrived_deleteExpired() {
        given(sender.enabled()).willReturn(true);
        given(tokenRepository.findQuestTargetTokens(Role.USER, TODAY, DONE)).willReturn(List.of("a", "old"));
        given(sender.send(List.of("a", "old"), PushMessage.quest()))
                .willReturn(new FcmSender.Result(1, 1, List.of("old")));

        notifier.questArrived();

        then(tokenRepository).should().deleteAllByTokenIn(List.of("old"));
    }

    @Test
    @DisplayName("Firebase 키 없음 → 대상 조회도 안 함")
    void questArrived_disabled() {
        given(sender.enabled()).willReturn(false);

        notifier.questArrived();

        then(tokenRepository).shouldHaveNoInteractions();
        then(sender).should(never()).send(anyList(), any());
    }

    @Test
    @DisplayName("편지 → 새로 만든 사람 토큰에만 편지 알림")
    void letterArrived() {
        given(sender.enabled()).willReturn(true);
        given(tokenRepository.findTokensByUserIds(List.of(1L, 2L))).willReturn(List.of("a"));
        given(sender.send(List.of("a"), PushMessage.letter())).willReturn(new FcmSender.Result(1, 0, List.of()));

        notifier.letterArrived(List.of(1L, 2L));

        then(sender).should().send(List.of("a"), PushMessage.letter());
    }

    @Test
    @DisplayName("편지를 새로 만든 사람 없음 → 아무것도 안 함")
    void letterArrived_empty() {
        given(sender.enabled()).willReturn(true);

        notifier.letterArrived(List.of());

        then(tokenRepository).shouldHaveNoInteractions();
        then(sender).should(never()).send(anyList(), any());
    }

    @Test
    @DisplayName("공지 → 알림 켠 일반 사용자 전부 · 결과 그대로 돌려줌")
    void notice() {
        PushMessage message = PushMessage.notice("서버 점검 안내", "내일 점검해요");
        FcmSender.Result result = new FcmSender.Result(3, 1, List.of("old"));
        given(sender.enabled()).willReturn(true);
        given(tokenRepository.findTokensByRole(Role.USER)).willReturn(List.of("a", "b", "c", "old"));
        given(sender.send(List.of("a", "b", "c", "old"), message)).willReturn(result);

        assertThat(notifier.notice(message)).isEqualTo(result);
        then(tokenRepository).should().deleteAllByTokenIn(List.of("old"));
    }

    @Test
    @DisplayName("공지 · Firebase 키 없음 → 0 (조회 안 함)")
    void notice_disabled() {
        given(sender.enabled()).willReturn(false);

        assertThat(notifier.notice(PushMessage.notice("점검", "내용"))).isEqualTo(FcmSender.Result.disabled());
        then(tokenRepository).shouldHaveNoInteractions();
    }
}