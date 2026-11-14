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

    @Bean
    RestClient aiRestClient(AiProperties props) {
        HttpClient http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1) // uvicorn 은 HTTP/2 업그레이드 미지원 → 본문이 사라져 422
                .connectTimeout(Duration.ofSeconds(3)) // 서버가 막 깨어났을 때도 연결되도록
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(props.timeout()); // 5초 넘으면 실패 → 대체값 // 3초 넘으면 실패 → 대체값

        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader("X-Internal-Key", props.internalKey())
                .requestFactory(factory)
                .build();
    }
}