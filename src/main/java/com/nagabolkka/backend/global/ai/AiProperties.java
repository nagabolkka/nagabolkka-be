package com.nagabolkka.backend.global.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "ai")
public record AiProperties(String baseUrl, String internalKey, Duration timeout, Duration letterTimeout,
                           Duration sttTimeout) {

    // yaml 에 없으면 20초
    public Duration letterTimeoutOrDefault() {
        return letterTimeout == null ? Duration.ofSeconds(20) : letterTimeout;
    }

    // yaml 에 없으면 10초 (녹음 30초까지 · OpenAI 음성 인식)
    public Duration sttTimeoutOrDefault() {
        return sttTimeout == null ? Duration.ofSeconds(10) : sttTimeout;
    }
}