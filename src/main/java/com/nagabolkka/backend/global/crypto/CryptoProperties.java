package com.nagabolkka.backend.global.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "crypto")
public record CryptoProperties(
        String hashSecret,
        String aesKey
) {
}