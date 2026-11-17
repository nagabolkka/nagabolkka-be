package com.nagabolkka.backend.domain.admin.service;

import com.nagabolkka.backend.domain.admin.dto.AdminUsersResponse;
import com.nagabolkka.backend.domain.admin.dto.AdminUsersResponse.PlusInterestSummary;
import com.nagabolkka.backend.domain.admin.dto.AdminUsersResponse.UserItem;
import com.nagabolkka.backend.domain.plus.repository.PlusInterestRepository;
import com.nagabolkka.backend.domain.quest.entity.QuestStatus;
import com.nagabolkka.backend.domain.quest.repository.DailyQuestRepository;
import com.nagabolkka.backend.domain.quest.repository.QuestStats;
import com.nagabolkka.backend.domain.user.entity.Role;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepository;
    private final DailyQuestRepository dailyQuestRepository;
    private final PlusInterestRepository plusInterestRepository;

    // 요약 + 사용자 별 현황 (USER 만 · 마음 기록 조회 안 함)
    public AdminUsersResponse getUsers() {
        Map<Long, QuestStats> stats = dailyQuestRepository
                .countQuestStats(Role.USER, QuestStatus.COMPLETED, QuestStatus.SKIPPED).stream()
                .collect(Collectors.toMap(QuestStats::userId, Function.identity()));

        List<UserItem> users = userRepository.findAllWithRegionByRole(Role.USER).stream()
                .map(user -> UserItem.of(user, stats.get(user.getId())))
                .toList();

        PlusInterestSummary plus = PlusInterestSummary.of(plusInterestRepository.countByOption(Role.USER));
        return AdminUsersResponse.of(users, plus);
    }
}