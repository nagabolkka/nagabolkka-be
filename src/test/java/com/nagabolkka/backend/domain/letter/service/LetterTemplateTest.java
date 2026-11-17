package com.nagabolkka.backend.domain.letter.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LetterTemplateTest {

    @Test
    @DisplayName("외출 0 → 쉬어갔구나")
    void closing_zero() {
        assertThat(LetterTemplate.closing(0)).isEqualTo("이번 주는 조용히 쉬어갔구나. 그것도 괜찮아.");
    }

    @Test
    @DisplayName("외출 1 ~ 2 → 한 걸음 한 걸음")
    void closing_few() {
        assertThat(LetterTemplate.closing(2)).isEqualTo("한 걸음 한 걸음이 다 소중했어.");
    }

    @Test
    @DisplayName("외출 3 이상 → 길이 되고 있어")
    void closing_many() {
        assertThat(LetterTemplate.closing(3)).isEqualTo("작은 걸음이 모여서 길이 되고 있어.");
    }

    @Test
    @DisplayName("본문 = 3문단 (AI 서버 템플릿과 같은 문장)")
    void of() {
        LetterStats stats = new LetterStats(3, 1, 8, 6, "바람의 숲", 40, 3.5);

        assertThat(LetterTemplate.of(stats)).isEqualTo(
                "이번 주엔 3번 바깥에 다녀왔어. 바람의 숲에 색이 40% 돌아왔고, 도감엔 조각이 6개 모였어.\n\n"
                        + "무리하지 않고 네 속도로 걸어온 게 느껴져.\n\n"
                        + "작은 걸음이 모여서 길이 되고 있어.");
    }
}