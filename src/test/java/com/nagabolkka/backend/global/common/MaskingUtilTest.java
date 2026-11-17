package com.nagabolkka.backend.global.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class MaskingUtilTest {

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "윤지, 윤*",
            "홍윤지, 홍*지",
            "남궁윤지, 남**지",
            "탐험가, 탐험가",
            "윤, 윤",
            "'  홍윤지  ', 홍*지"
    })
    @DisplayName("이름 마스킹 : 2 · 3 · 4글자 · 1글자 · 기본 이름 · 앞뒤 공백")
    void name(String input, String expected) {
        assertThat(MaskingUtil.name(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("null · 빈 이름 → 탐험가")
    void name_empty() {
        assertThat(MaskingUtil.name(null)).isEqualTo("탐험가");
        assertThat(MaskingUtil.name("   ")).isEqualTo("탐험가");
    }
}