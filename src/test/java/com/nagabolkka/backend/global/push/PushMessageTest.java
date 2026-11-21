package com.nagabolkka.backend.global.push;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PushMessageTest {

    @Test
    @DisplayName("퀘스트 · 편지 → 누르면 각 화면")
    void questAndLetter() {
        assertThat(PushMessage.quest().path()).isEqualTo("/quest");
        assertThat(PushMessage.letter().path()).isEqualTo("/letter");
    }

    @Test
    @DisplayName("공지 → 제목 앞 🔧 · 누르면 홈 · 짧은 내용은 그대로")
    void notice_short() {
        PushMessage message = PushMessage.notice("서버 점검 안내", "11월 28일 14:00 ~ 16:00 점검해요.");

        assertThat(message.title()).isEqualTo("🔧 서버 점검 안내");
        assertThat(message.body()).isEqualTo("11월 28일 14:00 ~ 16:00 점검해요.");
        assertThat(message.path()).isEqualTo("/");
    }

    @Test
    @DisplayName("공지 내용 100자 넘음 → 99자 + …")
    void notice_long() {
        String content = "가".repeat(150);

        PushMessage message = PushMessage.notice("서버 점검 안내", content);

        assertThat(message.body()).hasSize(100).endsWith("…");
        assertThat(message.body()).startsWith("가".repeat(99));
    }

    @Test
    @DisplayName("공지 내용 딱 100자 → 자르지 않음")
    void notice_exact100() {
        String content = "가".repeat(100);

        assertThat(PushMessage.notice("점검", content).body()).isEqualTo(content);
    }
}