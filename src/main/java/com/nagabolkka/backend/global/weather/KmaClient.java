package com.nagabolkka.backend.global.weather;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Slf4j
@Component
public class KmaClient {

    // 초단기실황 : 정시 자료가 40분쯤 올라옴
    static final int PUBLISH_MINUTE = 40;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HHmm");

    private final KmaProperties props;
    private final RestClient restClient;

    public KmaClient(KmaProperties props) {
        this.props = props;

        HttpClient http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(props.timeout()); // 2초 넘으면 실패 → 캐시값 · 맑음

        this.restClient = RestClient.builder()
                .baseUrl(props.baseUrl())
                .requestFactory(factory)
                .build();
    }

    // 지금 강수 (초단기실황 PTY) · 키 없음 · 실패 → empty
    public Optional<Weather> currentWeather(int nx, int ny, LocalDateTime now) {

        if (!props.hasKey()) {
            return Optional.empty();
        }
        LocalDateTime base = baseTime(now);
        try {
            KmaResponse res = restClient.get()
                    .uri(builder -> builder.path("/getUltraSrtNcst")
                            .queryParam("serviceKey", "{serviceKey}") // 값은 build 에서 넣음 → + / = 까지 인코딩
                            .queryParam("pageNo", 1)
                            .queryParam("numOfRows", 10)
                            .queryParam("dataType", "JSON")
                            .queryParam("base_date", base.format(DATE))
                            .queryParam("base_time", base.format(TIME))
                            .queryParam("nx", nx)
                            .queryParam("ny", ny)
                            .build(props.serviceKey()))
                    .retrieve()
                    .body(KmaResponse.class);

            if (res == null) {
                return Optional.empty();
            }
            Optional<String> pty = res.pty();
            if (pty.isEmpty()) {
                log.warn("기상청 응답 오류: resultCode {}", res.resultCode());
            }
            return pty.map(Weather::fromPty);
        } catch (RestClientException e) {
            // 메시지에 요청 주소(인증키 포함)가 들어갈 수 있음 → 예외 이름만
            log.warn("기상청 호출 실패: {} ({})", e.getClass().getSimpleName(),
                    e.getMostSpecificCause().getClass().getSimpleName());
            return Optional.empty();
        }
    }

    // 정시 기준 · 40분 전이면 1시간 전 (00:10 → 전날 23:00)
    static LocalDateTime baseTime(LocalDateTime now) {
        LocalDateTime hour = now.truncatedTo(ChronoUnit.HOURS);
        return now.getMinute() < PUBLISH_MINUTE ? hour.minusHours(1) : hour;
    }
}