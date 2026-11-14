package com.nagabolkka.backend.global.ai;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class CrisisFallback {

    // 일상 표현 · 낮출 표현은 먼저 지움
    private static final List<Pattern> REMOVE = List.of(
            Pattern.compile("자살골|자살예방|자해공갈|자살특공"),
            Pattern.compile("번개탄(구이|고기|불판|숯불|에고기)"),
            Pattern.compile("(창피|부끄|쪽팔|민망)[가-힣]{0,6}죽고싶"),
            Pattern.compile("죽고싶(진|지는|지)않"));

    private static final Map<String, Pattern> HIGH = new LinkedHashMap<>();

    static {
        HIGH.put("H01", Pattern.compile("죽고싶|죽고시프|주꼬싶|죽구싶|ㅈㄱㅅㄷ|ㅈㄱㅅㅇ"));
        HIGH.put("H02", Pattern.compile("자살(하고싶|할래|할거|하려|할까|해야겠)"));
        HIGH.put("H03", Pattern.compile("자해(하고싶|했|하려|할래|할거|하게돼)"));
        HIGH.put("H04", Pattern.compile("살기싫|살기가싫|살고싶지않|사는게의미없|사는게무의미"));
        HIGH.put("H05", Pattern.compile("죽어버리고싶|죽어버릴|죽으려고|죽으려구|죽을거야"));
        HIGH.put("H06", Pattern.compile("목숨을?끊|스스로목숨|생을?마감"));
        HIGH.put("H07", Pattern.compile("뛰어내리고싶|뛰어내리려|뛰어내릴|목을매|목매달|손목을?(긋|그었|그을)"));
        HIGH.put("H08", Pattern.compile("유서를?(쓰|썼|써)"));
        HIGH.put("H09", Pattern.compile("극단적인?선택을?(하고싶|할래|할거|하려)"));
        HIGH.put("H10", Pattern.compile("killmyself|wanttodie"));
    }

    public AiClient.CrisisResult check(String text) {
        String t = text.toLowerCase()
                .replaceAll("[^가-힣ㄱ-ㅎㅏ-ㅣa-z]", "")
                .replaceAll("(.)\\1{2,}", "$1");
        for (Pattern p : REMOVE) {
            t = p.matcher(t).replaceAll("");
        }
        for (Map.Entry<String, Pattern> e : HIGH.entrySet()) {
            if (e.getValue().matcher(t).find()) {
                return new AiClient.CrisisResult(true, "RULE", e.getKey(), null);
            }
        }
        return new AiClient.CrisisResult(false, null, null, null);
    }
}