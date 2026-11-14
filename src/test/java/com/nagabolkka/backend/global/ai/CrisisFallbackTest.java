package com.nagabolkka.backend.global.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class CrisisFallbackTest {

    CrisisFallback fallback = new CrisisFallback();

    @ParameterizedTest(name = "{0} → {1}")
    @DisplayName("AI 서버가 없을 때 Spring 위기 규칙")
    @CsvSource({
            "오늘 그냥 죽고 싶었어, true, H01",
            "죽 고 싶 다, true, H01",
            "살기 싫어, true, H04",
            "유서 써놨어, true, H08",
            "창피해서 죽고 싶었음, false, ",   // 낮출 표현 → 통과
            "배고파 죽겠다, false, ",
            "축구 보다가 자살골 나옴, false, ",
            "한강 걷고 왔다, false, "
    })
    void check(String text, boolean crisis, String ruleId) {
        AiClient.CrisisResult result = fallback.check(text);

        assertThat(result.crisis()).isEqualTo(crisis);
        assertThat(result.ruleId()).isEqualTo(ruleId);
    }
}