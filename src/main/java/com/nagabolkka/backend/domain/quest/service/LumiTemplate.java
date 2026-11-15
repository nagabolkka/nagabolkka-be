package com.nagabolkka.backend.domain.quest.service;

// 루미 한마디 기본 문장 (AI 실패 시)
final class LumiTemplate {

    private static final int QUOTE_MAX = 20;

    private LumiTemplate() {
    }

    static String of(String reflection, String pieceName, String restoreText) {
        if (reflection != null) {
            String quote = reflection.length() <= QUOTE_MAX ? reflection : reflection.substring(0, QUOTE_MAX) + "…";
            return "\"" + quote + "\" ㅡ 그 말 기억할게. 네가 가져온 " + pieceName + " 덕분에 " + restoreText + "!";
        }
        return "오늘도 다녀와 줘서 고마워. 네가 가져온 " + pieceName + " 덕분에 " + restoreText + "!";
    }
}