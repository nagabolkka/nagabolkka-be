package com.nagabolkka.backend.domain.auth.kakao;

import com.nagabolkka.backend.domain.auth.kakao.dto.KakaoTokenResponse;
import com.nagabolkka.backend.domain.auth.kakao.dto.KakaoUserResponse;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

@Slf4j
@Component
public class KakaoOAuthClient {

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final KakaoProperties kakaoProperties;
    private final RestClient restClient;

    public KakaoOAuthClient(KakaoProperties kakaoProperties) {
        this.kakaoProperties = kakaoProperties;

        // 카카오가 느릴 때 요청이 오래 매달리지 않도록 타임아웃 설정
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public KakaoUserInfo getUserInfo(String code, String redirectUri) {
        try {
            String accessToken = requestAccessToken(code, redirectUri);
            KakaoUserResponse user = restClient.get()
                    .uri(USER_INFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(KakaoUserResponse.class);

            if (user == null || user.id() == null) {
                throw new CustomException(ErrorCode.KAKAO_LOGIN_FAILED);
            }
            return new KakaoUserInfo(user.id(), user.nickname());
        } catch (RestClientException e) {
            // 인가 코드 만료 · 재사용, redirect_uri 불일치, 키 오류, 카카오 장애 등
            log.warn("카카오 로그인 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.KAKAO_LOGIN_FAILED);
        }
    }

    private String requestAccessToken(String code, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", kakaoProperties.restApiKey());
        form.add("redirect_uri", redirectUri);
        form.add("code", code);
        if (kakaoProperties.clientSecret() != null && !kakaoProperties.clientSecret().isBlank()) {
            form.add("client_secret", kakaoProperties.clientSecret());
        }

        KakaoTokenResponse token = restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(KakaoTokenResponse.class);

        if (token == null || token.accessToken() == null) {
            throw new CustomException(ErrorCode.KAKAO_LOGIN_FAILED);
        }
        return token.accessToken();
    }
}
