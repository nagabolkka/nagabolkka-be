package com.nagabolkka.backend.domain.letter.service;

// AI 서버가 꺼졌을 때 · 활동 없는 주
public final class LetterTemplate {

    private LetterTemplate() {
    }

    public static String of(LetterStats stats) {
        return String.join("\n\n",
                "이번 주엔 " + stats.outingCount() + "번 바깥에 다녀왔어. " + stats.regionName() + "에 색이 "
                        + stats.restoration() + "% 돌아왔고, 도감엔 조각이 " + stats.foundSlots() + "개 모였어.",
                "무리하지 않고 네 속도로 걸어온 게 느껴져.",
                closing(stats.outingCount()));
    }

    static String closing(int outingCount) {
        if (outingCount == 0) {
            return "이번 주는 조용히 쉬어갔구나. 그것도 괜찮아.";
        }
        if (outingCount <= 2) {
            return "한 걸음 한 걸음이 다 소중했어.";
        }
        return "작은 걸음이 모여서 길이 되고 있어.";
    }
}