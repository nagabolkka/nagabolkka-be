package com.nagabolkka.backend.global.push;

// 잠금 화면에 보여도 되는 말만 (기분 · 회고 · 미션 결과 X) · path = 누르면 열 화면
public record PushMessage(String title, String body, String path) {

    static final int BODY_MAX = 100; // 잠금 화면에서 잘리는 길이 감안

    public static PushMessage quest() {
        return new PushMessage("오늘의 퀘스트가 도착했어요 🌙",
                "오늘은 어떤 문을 열어 볼까? 루미가 기다리고 있어.", "/quest");
    }

    public static PushMessage letter() {
        return new PushMessage("루미가 편지를 남겼어요 💌",
                "지난주 모험을 돌아봤어. 열어 볼래?", "/letter");
    }

    // 서버 점검 등 관리자 공지 → 누르면 홈 (공지 팝업)
    public static PushMessage notice(String title, String content) {
        String body = content.length() > BODY_MAX ? content.substring(0, BODY_MAX - 1) + "…" : content;
        return new PushMessage("🔧 " + title, body, "/");
    }
}