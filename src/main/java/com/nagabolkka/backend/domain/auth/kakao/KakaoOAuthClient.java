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
    private static final String UNLINK_URL = "https://kapi.kakao.com/v1/user/unlink";

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

    // 탈퇴 : 카카오 연결 끊기 (Admin 키). 실패해도 예외 없이 false → 탈퇴는 그대로 진행
    public boolean unlink(Long kakaoId) {
        if (!kakaoProperties.hasAdminKey()) {
            log.warn("카카오 연결 끊기 건너뜀: KAKAO_ADMIN_KEY 없음");
            return false;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("target_id_type", "user_id");
        form.add("target_id", String.valueOf(kakaoId));

        try {
            restClient.post()
                    .uri(UNLINK_URL)
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoProperties.adminKey())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            // 회원번호 · 키는 남기지 않음 (예외 이름만). 이미 끊긴 사용자도 여기로 옴
            log.warn("카카오 연결 끊기 실패: {}", e.getClass().getSimpleName());
            return false;
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