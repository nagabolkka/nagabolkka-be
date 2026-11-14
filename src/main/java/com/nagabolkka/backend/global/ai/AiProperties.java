package com.nagabolkka.backend.global.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "ai")
public record AiProperties(String baseUrl, String internalKey, Duration timeout) {
}