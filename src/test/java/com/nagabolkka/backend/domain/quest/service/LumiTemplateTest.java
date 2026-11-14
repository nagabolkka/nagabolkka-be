package com.nagabolkka.backend.domain.quest.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LumiTemplateTest {

    @Test
    @DisplayName("회고가 있으면 인용")
    void withReflection() {
        assertThat(LumiTemplate.of("바람이 시원했다", "햇살 조각", "하늘에 색이 돌아왔어"))
                .isEqualTo("\"바람이 시원했다\" ㅡ 그 말 기억할게. 네가 가져온 햇살 조각 덕분에 하늘에 색이 돌아왔어!");
    }

    @Test
    @DisplayName("긴 회고는 20자 + … 로 자름")
    void longReflection() {
        assertThat(LumiTemplate.of("가".repeat(30), "햇살 조각", "하늘에 색이 돌아왔어"))
                .startsWith("\"" + "가".repeat(20) + "…\"");
    }

    @Test
    @DisplayName("회고가 없으면 기본 문장")
    void withoutReflection() {
        assertThat(LumiTemplate.of(null, "햇살 조각", "하늘에 색이 돌아왔어"))
                .isEqualTo("오늘도 다녀와 줘서 고마워. 네가 가져온 햇살 조각 덕분에 하늘에 색이 돌아왔어!");
    }
}