package com.nagabolkka.backend.global.ai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    // 사용자가 기다리는 호출 (위기 검사 · 루미 한마디 · 연습) : 5초 넘으면 대체값
    @Bean
    RestClient aiRestClient(AiProperties props) {
        return build(props, props.timeout());
    }

    // 주간 편지 배치 : 20초 (새벽 · 아무도 기다리지 않음)
    @Bean
    RestClient aiLetterRestClient(AiProperties props) {
        return build(props, props.letterTimeoutOrDefault());
    }

    private static RestClient build(AiProperties props, Duration readTimeout) {
        HttpClient http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1) // uvicorn 은 HTTP/2 업그레이드 미지원 → 본문이 사라져 422
                .connectTimeout(Duration.ofSeconds(3)) // 서버가 막 깨어났을 때도 연결되도록
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader("X-Internal-Key", props.internalKey())
                .requestFactory(factory)
                .build();
    }
}