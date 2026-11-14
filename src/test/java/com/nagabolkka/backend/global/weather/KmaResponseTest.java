package com.nagabolkka.backend.global.weather;

import com.nagabolkka.backend.global.weather.KmaResponse.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KmaResponseTest {

    private KmaResponse response(String code, List<Item> items) {
        return new KmaResponse(new Response(new Header(code, "msg"), new Body(new Items(items))));
    }

    @Test
    void 정상이면_PTY() {
        KmaResponse res = response("00", List.of(new Item("T1H", "8.3"), new Item("PTY", "1")));

        assertThat(res.pty()).contains("1");
    }

    @Test
    void 오류_코드면_empty() {
        assertThat(response("03", List.of(new Item("PTY", "0"))).pty()).isEmpty(); // NO_DATA
    }

    @Test
    void PTY_가_없으면_empty() {
        assertThat(response("00", List.of(new Item("T1H", "8.3"))).pty()).isEmpty();
    }

    @Test
    void 빈_응답이면_empty() {
        assertThat(new KmaResponse(null).pty()).isEmpty();
        assertThat(response("00", null).pty()).isEmpty();
    }
}