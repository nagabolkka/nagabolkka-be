package com.nagabolkka.backend.domain.admin.service;

import com.nagabolkka.backend.domain.admin.dto.AdminUsersResponse;
import com.nagabolkka.backend.domain.admin.dto.AdminUsersResponse.UserItem;
import com.nagabolkka.backend.domain.district.entity.District;
import com.nagabolkka.backend.domain.goal.entity.Goal;
import com.nagabolkka.backend.domain.plus.entity.PriceOption;
import com.nagabolkka.backend.domain.plus.repository.PlusInterestRepository;
import com.nagabolkka.backend.domain.plus.repository.PriceOptionCount;
import com.nagabolkka.backend.domain.quest.entity.QuestStatus;
import com.nagabolkka.backend.domain.quest.repository.DailyQuestRepository;
import com.nagabolkka.backend.domain.quest.repository.QuestStats;
import com.nagabolkka.backend.domain.region.entity.Region;
import com.nagabolkka.backend.domain.user.entity.Role;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock UserRepository userRepository;
    @Mock DailyQuestRepository dailyQuestRepository;
    @Mock PlusInterestRepository plusInterestRepository;

    @InjectMocks AdminUserService adminUserService;

    private User user(Long id, String nickname, boolean onboarded) {
        User user = User.createKakaoUser(nickname);
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "nickname", nickname); // 가입 때 이름 변환과 상관없이 고정
        if (onboarded) {
            Region stage1 = Region.of(1, "시작의 방", "집 안", "빛의 조각", "#E8C06A", "a.png", "b.png");
            District seongbuk = District.of("성북구", new BigDecimal("37.5894"), new BigDecimal("127.0167"), 61, 127);
            user.completeOnboarding(2, stage1, Goal.of("JOB", "취업"), seongbuk);
        }
        return user;
    }

    private void givenData(List<User> users, List<QuestStats> stats, List<PriceOptionCount> plus) {
        given(userRepository.findAllWithRegionByRole(Role.USER)).willReturn(users);
        given(dailyQuestRepository.countQuestStats(Role.USER, QuestStatus.COMPLETED, QuestStatus.SKIPPED)).willReturn(stats);
        given(plusInterestRepository.countByOption(Role.USER)).willReturn(plus);
    }

    @Test
    @DisplayName("사용자별 마스킹 · 달성률 반올림 · 퀘스트 없음 0 · 평균은 배정 있는 사용자만")
    void getUsers() {
        User hong = user(1L, "홍윤지", true);
        hong.updateLastActiveAt(LocalDateTime.of(2026, 11, 14, 21, 30));
        User newbie = user(2L, "윤지", false);          // 가입만 · 퀘스트 없음
        User namgung = user(3L, "남궁윤지", true);
        givenData(List.of(hong, newbie, namgung),
                List.of(new QuestStats(1L, 10L, 7L, 2L), new QuestStats(3L, 3L, 1L, 1L)),
                List.of());

        AdminUsersResponse res = adminUserService.getUsers();

        assertThat(res.users()).extracting(UserItem::label).containsExactly("홍*지", "윤*", "남**지");
        assertThat(res.users()).extracting(UserItem::achievementRate).containsExactly(70, 0, 33);

        UserItem first = res.users().get(0);
        assertThat(first.stageNo()).isEqualTo(1);
        assertThat(first.assigned()).isEqualTo(10);
        assertThat(first.completed()).isEqualTo(7);
        assertThat(first.skipped()).isEqualTo(2);
        assertThat(first.lastActiveAt()).isEqualTo(LocalDate.of(2026, 11, 14));

        UserItem second = res.users().get(1);
        assertThat(second.stageNo()).isNull();
        assertThat(second.lastActiveAt()).isNull();

        assertThat(res.summary().totalUsers()).isEqualTo(3);
        assertThat(res.summary().avgAchievementRate()).isEqualTo(52); // (70 + 33) / 2 = 51.5 → 52 (가입만 한 사람 제외)
    }

    @Test
    @DisplayName("플러스 집계 : 3개 선택지 모두 (0 포함) · 전체 수 · 직접 입력 평균 반올림")
    void plusSummary() {
        givenData(List.of(), List.of(), List.of(
                new PriceOptionCount(PriceOption.PRICE_1900, 2L, null),
                new PriceOptionCount(PriceOption.CUSTOM, 2L, 2499.5)));

        AdminUsersResponse res = adminUserService.getUsers();

        var plus = res.summary().plusInterest();
        assertThat(plus.total()).isEqualTo(4);
        assertThat(plus.byOption()).containsEntry(PriceOption.PRICE_1900, 2L)
                .containsEntry(PriceOption.PRICE_3900, 0L)
                .containsEntry(PriceOption.CUSTOM, 2L);
        assertThat(plus.customPriceAvg()).isEqualTo(2500);
        assertThat(res.summary().avgAchievementRate()).isZero(); // 사용자 없음
    }
}