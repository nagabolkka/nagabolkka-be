package com.nagabolkka.backend.global.push;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class FcmSenderTest {

    private static final String APP_URL = "https://nagabolkka.vercel.app";
    private static final PushMessage MESSAGE = PushMessage.quest();

    @Mock FirebaseMessaging messaging;
    @Mock ObjectProvider<FirebaseMessaging> provider;

    FcmSender sender;

    @BeforeEach
    void setUp() {
        given(provider.getIfAvailable()).willReturn(messaging);
        sender = new FcmSender(provider, APP_URL);
    }

    private static List<String> tokens(int count) {
        return IntStream.range(0, count).mapToObj(i -> "token-" + i).toList();
    }

    private static SendResponse ok() {
        SendResponse response = mock(SendResponse.class);
        given(response.isSuccessful()).willReturn(true);
        return response;
    }

    private static SendResponse fail(MessagingErrorCode code) {
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        given(exception.getMessagingErrorCode()).willReturn(code);
        SendResponse response = mock(SendResponse.class);
        given(response.isSuccessful()).willReturn(false);
        given(response.getException()).willReturn(exception);
        return response;
    }

    private static BatchResponse batch(List<SendResponse> responses) {
        BatchResponse batch = mock(BatchResponse.class);
        given(batch.getResponses()).willReturn(responses);
        return batch;
    }

    @Test
    @DisplayName("서비스 계정 키 없음 → 아무것도 안 보내고 0")
    void disabled() throws Exception {
        ObjectProvider<FirebaseMessaging> empty = mock(ObjectProvider.class);
        given(empty.getIfAvailable()).willReturn(null);
        FcmSender noKey = new FcmSender(empty, APP_URL);

        assertThat(noKey.enabled()).isFalse();
        assertThat(noKey.send(tokens(3), MESSAGE)).isEqualTo(FcmSender.Result.disabled());
    }

    @Test
    @DisplayName("토큰 0개 → FCM 안 부름")
    void emptyTokens() throws Exception {
        assertThat(sender.send(List.of(), MESSAGE)).isEqualTo(FcmSender.Result.disabled());
        then(messaging).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("501개 → 500 + 1 로 나눠 두 번 보냄")
    @SuppressWarnings("unchecked")
    void chunk500() throws Exception {
        given(messaging.sendEach(anyList())).willAnswer(invocation -> {
            List<Message> messages = invocation.getArgument(0);
            return batch(messages.stream().map(m -> ok()).toList());
        });

        FcmSender.Result result = sender.send(tokens(501), MESSAGE);

        ArgumentCaptor<List<Message>> captor = ArgumentCaptor.forClass(List.class);
        then(messaging).should(times(2)).sendEach(captor.capture());
        assertThat(captor.getAllValues()).extracting(List::size).containsExactly(500, 1);
        assertThat(result).isEqualTo(new FcmSender.Result(501, 0, List.of()));
    }

    @Test
    @DisplayName("UNREGISTERED · INVALID_ARGUMENT → 만료 목록 · 다른 실패는 실패만")
    void expiredTokens() throws Exception {
        // mock 을 먼저 다 만든 뒤 given (given 안에서 mock 을 만들면 UnfinishedStubbingException)
        BatchResponse responses = batch(List.of(
                ok(),
                fail(MessagingErrorCode.UNREGISTERED),
                fail(MessagingErrorCode.INVALID_ARGUMENT),
                fail(MessagingErrorCode.UNAVAILABLE)));
        given(messaging.sendEach(anyList())).willReturn(responses);

        FcmSender.Result result = sender.send(tokens(4), MESSAGE);

        assertThat(result.sent()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(3);
        assertThat(result.expiredTokens()).containsExactly("token-1", "token-2");
    }

    @Test
    @DisplayName("한 묶음이 통째로 실패해도 다음 묶음은 계속")
    @SuppressWarnings("unchecked")
    void chunkFails_continue() throws Exception {
        List<Integer> calls = new ArrayList<>();
        given(messaging.sendEach(anyList())).willAnswer(invocation -> {
            List<Message> messages = invocation.getArgument(0);
            calls.add(messages.size());
            if (calls.size() == 1) {
                throw mock(FirebaseMessagingException.class);
            }
            return batch(messages.stream().map(m -> ok()).toList());
        });

        FcmSender.Result result = sender.send(tokens(501), MESSAGE);

        assertThat(calls).containsExactly(500, 1);
        assertThat(result).isEqualTo(new FcmSender.Result(1, 500, List.of()));
    }
}