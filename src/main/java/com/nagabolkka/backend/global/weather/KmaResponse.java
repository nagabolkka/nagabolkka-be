package com.nagabolkka.backend.global.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Optional;

// 기상청 응답 중 필요한 부분만 (header.resultCode · items.item[].category · obsrValue)
@JsonIgnoreProperties(ignoreUnknown = true)
public record KmaResponse(Response response) {

    static final String OK = "00";

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(Header header, Body body) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(String resultCode, String resultMsg) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(Items items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Items(List<Item> item) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(String category, String obsrValue) {
    }

    // 정상 응답의 PTY 값 · 오류 코드 · 빈 응답이면 empty
    public Optional<String> pty() {
        if (response == null || response.header() == null || !OK.equals(response.header().resultCode())) {
            return Optional.empty();
        }
        if (response.body() == null || response.body().items() == null || response.body().items().item() == null) {
            return Optional.empty();
        }
        return response.body().items().item().stream()
                .filter(item -> "PTY".equals(item.category()))
                .map(Item::obsrValue)
                .findFirst();
    }

    public String resultCode() {
        return response == null || response.header() == null ? null : response.header().resultCode();
    }
}