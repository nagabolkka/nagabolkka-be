package com.nagabolkka.backend.global.kakao;

import com.nagabolkka.backend.global.kakao.KakaoLocalResponse.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KakaoLocalResponseTest {

    @Test
    void 첫_번째_장소() {
        KakaoLocalResponse res = new KakaoLocalResponse(List.of(
                new Document("123", "GS25 종암점", "127.0321", "37.5921"),
                new Document("456", "CU 성북점", "127.0400", "37.6000")));

        KakaoPlace place = res.first().orElseThrow();

        assertThat(place.id()).isEqualTo("123");
        assertThat(place.name()).isEqualTo("GS25 종암점");
        assertThat(place.lat()).isEqualByComparingTo("37.5921"); // y = 위도
        assertThat(place.lng()).isEqualByComparingTo("127.0321"); // x = 경도
    }

    @Test
    void 결과가_없으면_empty() {
        assertThat(new KakaoLocalResponse(List.of()).first()).isEmpty();
        assertThat(new KakaoLocalResponse(null).first()).isEmpty();
    }

    @Test
    void 좌표가_이상하면_empty() {
        assertThat(new KakaoLocalResponse(List.of(new Document("1", "어딘가", "abc", "37.5"))).first()).isEmpty();
        assertThat(new KakaoLocalResponse(List.of(new Document("1", "어딘가", null, "37.5"))).first()).isEmpty();
    }
}