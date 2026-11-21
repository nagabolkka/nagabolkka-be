package com.nagabolkka.backend.domain.practice.service;

import com.nagabolkka.backend.domain.practice.dto.TranscribeResponse;
import com.nagabolkka.backend.global.ai.AiClient;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PracticeVoiceServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long SESSION_ID = 55L;
    private static final byte[] AUDIO = new byte[]{1, 2, 3};

    @Mock PracticeSessionManager sessionManager;
    @Mock AiClient aiClient;

    PracticeVoiceService service;

    @BeforeEach
    void setUp() {
        service = new PracticeVoiceService(sessionManager, aiClient);
    }

    private static MockMultipartFile file(String contentType, byte[] bytes) {
        return new MockMultipartFile("audio", "voice", contentType, bytes);
    }

    @Test
    @DisplayName("크롬 webm (codecs 붙음) → 앞뒤 공백 없는 글자 · 파일 이름 voice.webm")
    void webm() {
        given(aiClient.transcribe(AUDIO, "voice.webm", "audio/webm"))
                .willReturn(Optional.of(new AiClient.TranscribeResult("  네, 신청할게요.  ", false)));

        TranscribeResponse res = service.transcribe(USER_ID, SESSION_ID, file("audio/webm;codecs=opus", AUDIO));

        assertThat(res.text()).isEqualTo("네, 신청할게요.");
        then(sessionManager).should().checkOpen(USER_ID, SESSION_ID);
    }

    @Test
    @DisplayName("아이폰 mp4 → 파일 이름 voice.mp4")
    void mp4() {
        given(aiClient.transcribe(AUDIO, "voice.mp4", "audio/mp4"))
                .willReturn(Optional.of(new AiClient.TranscribeResult("안녕하세요", false)));

        assertThat(service.transcribe(USER_ID, SESSION_ID, file("audio/mp4", AUDIO)).text()).isEqualTo("안녕하세요");
    }

    @Test
    @DisplayName("AI 실패 · 빈 글자 → text null (5xx 아님)")
    void aiFail() {
        given(aiClient.transcribe(any(), anyString(), anyString()))
                .willReturn(Optional.empty())
                .willReturn(Optional.of(new AiClient.TranscribeResult("   ", true)));

        assertThat(service.transcribe(USER_ID, SESSION_ID, file("audio/webm", AUDIO)).text()).isNull();
        assertThat(service.transcribe(USER_ID, SESSION_ID, file("audio/webm", AUDIO)).text()).isNull();
    }

    @Test
    @DisplayName("200자 넘으면 200자까지")
    void cutTo200() {
        given(aiClient.transcribe(any(), anyString(), anyString()))
                .willReturn(Optional.of(new AiClient.TranscribeResult("가".repeat(250), false)));

        assertThat(service.transcribe(USER_ID, SESSION_ID, file("audio/webm", AUDIO)).text()).hasSize(200);
    }

    @Test
    @DisplayName("빈 파일 · 오디오 아님 · 2MB 초과 → 400 · 세션 확인 · AI 안 부름")
    void invalidFile() {
        byte[] big = new byte[(int) PracticeVoiceService.MAX_BYTES + 1];

        for (MockMultipartFile bad : new MockMultipartFile[]{
                file("audio/webm", new byte[0]), file("image/png", AUDIO), file(null, AUDIO), file("audio/webm", big)}) {
            assertThatThrownBy(() -> service.transcribe(USER_ID, SESSION_ID, bad))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
        }
        then(sessionManager).shouldHaveNoInteractions();
        then(aiClient).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("끝난 세션 (409) → AI 안 부름")
    void closedSession() {
        willThrow(new CustomException(ErrorCode.PRACTICE_SESSION_CLOSED))
                .given(sessionManager).checkOpen(USER_ID, SESSION_ID);

        assertThatThrownBy(() -> service.transcribe(USER_ID, SESSION_ID, file("audio/webm", AUDIO)))
                .extracting("errorCode").isEqualTo(ErrorCode.PRACTICE_SESSION_CLOSED);
        then(aiClient).should(never()).transcribe(any(), eq("voice.webm"), anyString());
    }
}