package com.nagabolkka.backend.domain.user.entity;

import com.nagabolkka.backend.global.common.Difficulty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import com.nagabolkka.backend.domain.district.entity.District;
import com.nagabolkka.backend.domain.goal.entity.Goal;
import com.nagabolkka.backend.domain.region.entity.Region;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @ParameterizedTest(name = "\"{0}\" → \"{1}\"")
    @CsvSource({
            "홍윤지, 윤지",     // 한글 3글자 → 성 떼기
            "'  홍윤지 ', 윤지", // 앞뒤 공백 제거 후 판단
            "윤지, 윤지",       // 2글자는 그대로
            "남궁민수, 남궁민수", // 4글자는 그대로
            "Yoonji, Yoonji",  // 영문은 그대로
            "홍윤1, 홍윤1"      // 한글만 3글자가 아니면 그대로
    })
    @DisplayName("닉네임 변환 규칙")
    void toDisplayName(String kakaoNickname, String expected) {
        assertThat(User.toDisplayName(kakaoNickname)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("닉네임 미동의 · 빈 값이면 탐험가")
    void toDisplayName_blank(String kakaoNickname) {
        assertThat(User.toDisplayName(kakaoNickname)).isEqualTo(User.DEFAULT_NICKNAME);
    }

    @Test
    @DisplayName("30자를 넘으면 30자로 자른다")
    void toDisplayName_tooLong() {
        String longName = "a".repeat(40);
        assertThat(User.toDisplayName(longName)).hasSize(30);
    }

    @Test
    @DisplayName("가입 직후 기본값 : USER · EASY · 온보딩 전")
    void createKakaoUser_defaults() {
        User user = User.createKakaoUser("홍윤지");

        assertThat(user.getNickname()).isEqualTo("윤지");
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getDifficulty()).isEqualTo(Difficulty.EASY);
        assertThat(user.isOnboardingCompleted()).isFalse();
    }

    @Test
    @DisplayName("시작 체크 결과를 저장하면 온보딩 완료")
    void completeOnboarding() {
        User user = User.createKakaoUser("홍윤지");
        Region region = Region.of(1, "시작의 방", "집 안", "빛의 조각", "#E8C06A", "a.png", "b.png");
        Goal goal = Goal.of("JOB", "취업");
        District district = District.of("성북구", new BigDecimal("37.5894"), new BigDecimal("127.0167"), 61, 127);

        user.completeOnboarding(3, region, goal, district);

        assertThat(user.isOnboardingCompleted()).isTrue();
        assertThat(user.getStartScore()).isEqualTo(3);
        assertThat(user.getCurrentRegion()).isSameAs(region);
        assertThat(user.getCurrentGoal()).isSameAs(goal);
        assertThat(user.getDistrict()).isSameAs(district);
    }
}