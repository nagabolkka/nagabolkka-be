package com.nagabolkka.backend.domain.auth.kakao;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "kakao")
public record KakaoProperties(
        String restApiKey,
        String clientSecret,
        List<String> allowedRedirectUris
) {
    public KakaoProperties {
        allowedRedirectUris = allowedRedirectUris == null ? List.of() : List.copyOf(allowedRedirectUris);
    }
}