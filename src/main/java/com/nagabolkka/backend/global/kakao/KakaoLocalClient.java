package com.nagabolkka.backend.global.kakao;

import com.nagabolkka.backend.domain.auth.kakao.KakaoProperties;
import com.nagabolkka.backend.global.geo.GeoPoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
public class KakaoLocalClient {

    private static final String KEYWORD_URL = "https://dapi.kakao.com/v2/local/search/keyword.json";

    private final KakaoProperties kakaoProperties;
    private final RestClient restClient;

    public KakaoLocalClient(KakaoProperties kakaoProperties) {
        this.kakaoProperties = kakaoProperties;

        // 퀘스트 배정 안에서 부름 → 짧게 (실패하면 기본 장소 이름)
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(2));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    // 기준 위치에서 반경 안 가장 가까운 장소 1개 · 키 없음 · 실패 · 결과 없음 → empty
    public Optional<KakaoPlace> searchNearest(String query, GeoPoint origin, int radiusM) {
        String key = kakaoProperties.restApiKey();
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        try {
            KakaoLocalResponse res = restClient.get()
                    .uri(KEYWORD_URL + "?query={query}&x={x}&y={y}&radius={radius}&sort=distance&size=1",
                            query, origin.lng(), origin.lat(), radiusM) // x = 경도 · y = 위도
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + key)
                    .retrieve()
                    .body(KakaoLocalResponse.class);
            return res == null ? Optional.empty() : res.first();
        } catch (RestClientException e) {
            // 요청 주소에 사용자 좌표가 들어감 → 예외 이름만 (키 · 좌표 · 검색어 안 남김)
            log.warn("카카오 로컬 호출 실패: {} ({})", e.getClass().getSimpleName(),
                    e.getMostSpecificCause().getClass().getSimpleName());
            return Optional.empty();
        }
    }
}