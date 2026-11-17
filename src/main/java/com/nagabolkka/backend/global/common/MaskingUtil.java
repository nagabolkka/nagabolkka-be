package com.nagabolkka.backend.global.common;

// 관리자 · Swagger 응답용 이름 마스킹
// 2글자 윤* · 3글자 홍*지 · 4글자 이상 남**지 · 1글자 · 기본 이름 "탐험가" 는 그대로
public final class MaskingUtil {

    private static final String DEFAULT_NAME = "탐험가";
    private static final String MASK = "*";

    private MaskingUtil() {
    }

    public static String name(String name) {
        if (name == null || name.isBlank()) {
            return DEFAULT_NAME;
        }
        String trimmed = name.strip();
        if (trimmed.equals(DEFAULT_NAME)) {
            return trimmed;
        }

        // 이모지 · 한자도 한 글자로 세기 (char 가 아니라 code point)
        int[] chars = trimmed.codePoints().toArray();
        int length = chars.length;
        if (length <= 1) {
            return trimmed;
        }
        String first = new String(chars, 0, 1);
        if (length == 2) {
            return first + MASK;
        }
        String last = new String(chars, length - 1, 1);
        return first + MASK.repeat(length - 2) + last;
    }
}