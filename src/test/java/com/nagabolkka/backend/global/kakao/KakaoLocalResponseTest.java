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

    @Test
    void 전부_돌려주고_좌표가_이상한_것만_뺌() {
        KakaoLocalResponse res = new KakaoLocalResponse(List.of(
                new Document("1", "아리랑도서관", "문화,예술 > 도서관", "서울 성북구 정릉동 1", "서울 성북구 아리랑로 82",
                        "127.0139784", "37.5998972"),
                new Document("2", "이상한 곳", null, null, null, "abc", "37.5"),
                new Document("3", "개운산근린공원", "여행 > 공원", "서울 성북구 돈암동 1", null, "127.0200", "37.5950")));

        List<KakaoSearchPlace> all = res.all();

        assertThat(all).extracting(KakaoSearchPlace::id).containsExactly("1", "3");
        assertThat(all.get(0).categoryName()).isEqualTo("문화,예술 > 도서관");
        assertThat(all.get(0).lat()).isEqualByComparingTo("37.5998972");
    }

    @Test
    void 주소는_도로명_먼저_없으면_지번() {
        KakaoLocalResponse res = new KakaoLocalResponse(List.of(
                new Document("1", "a", null, "지번 주소", "도로명 주소", "127.0", "37.5"),
                new Document("2", "b", null, "지번 주소", " ", "127.0", "37.5"),
                new Document("3", "c", null, null, null, "127.0", "37.5")));

        assertThat(res.all()).extracting(KakaoSearchPlace::address)
                .containsExactly("도로명 주소", "지번 주소", "");
    }
}