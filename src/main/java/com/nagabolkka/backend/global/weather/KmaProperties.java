package com.nagabolkka.backend.global.weather;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

// 기상청 단기예보 조회서비스 (공공데이터포털)
@ConfigurationProperties(prefix = "kma")
public record KmaProperties(String serviceKey, String baseUrl, Duration timeout) {

    public boolean hasKey() {
        return serviceKey != null && !serviceKey.isBlank();
    }
}