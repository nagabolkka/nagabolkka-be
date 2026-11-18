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
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class KakaoLocalClient {

    private static final String KEYWORD_URL = "https://dapi.kakao.com/v2/local/search/keyword.json";
    static final int PAGE_SIZE = 15; // 카카오 키워드 검색 한 번 최대

    private final KakaoProperties kakaoProperties;
    private final RestClient restClient;

    public KakaoLocalClient(KakaoProperties kakaoProperties) {
        this.kakaoProperties = kakaoProperties;

        // 퀘스트 배정 · 지도 조회 안에서 부름 → 짧게 (실패하면 기본 장소 · 저장된 장소)
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(2));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    // 퀘스트 : 반경 안 가장 가까운 장소 1개 · 키 없음 · 실패 · 결과 없음 → empty
    public Optional<KakaoPlace> searchNearest(String query, GeoPoint origin, int radiusM) {
        return search(query, origin, radiusM, 1).flatMap(KakaoLocalResponse::first);
    }

    // 지도 : 반경 안 장소 최대 15개 (거리순) · 키 없음 · 실패 → empty / 결과 0개 → 빈 리스트
    public Optional<List<KakaoSearchPlace>> searchAll(String query, GeoPoint origin, int radiusM) {
        return search(query, origin, radiusM, PAGE_SIZE).map(KakaoLocalResponse::all);
    }

    private Optional<KakaoLocalResponse> search(String query, GeoPoint origin, int radiusM, int size) {
        String key = kakaoProperties.restApiKey();
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        try {
            KakaoLocalResponse res = restClient.get()
                    .uri(KEYWORD_URL + "?query={query}&x={x}&y={y}&radius={radius}&sort=distance&size={size}",
                            query, origin.lng(), origin.lat(), radiusM, size) // x = 경도 · y = 위도
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + key)
                    .retrieve()
                    .body(KakaoLocalResponse.class);
            return Optional.ofNullable(res);
        } catch (RestClientException e) {
            // 요청 주소에 사용자 좌표가 들어감 → 예외 이름만 (키 · 좌표 · 검색어 안 남김)
            log.warn("카카오 로컬 호출 실패: {} ({})", e.getClass().getSimpleName(),
                    e.getMostSpecificCause().getClass().getSimpleName());
            return Optional.empty();
        }
    }
}