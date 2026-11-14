package com.nagabolkka.backend.domain.user.service;

import com.nagabolkka.backend.domain.goal.entity.Goal;
import com.nagabolkka.backend.domain.goal.repository.GoalRepository;
import com.nagabolkka.backend.domain.piece.repository.PieceAcquisitionRepository;
import com.nagabolkka.backend.domain.user.dto.MyInfoResponse;
import com.nagabolkka.backend.domain.user.dto.UpdateMyInfoRequest;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final GoalRepository goalRepository;
    private final PieceAcquisitionRepository pieceAcquisitionRepository;

    // 내 정보 (마이페이지)
    public MyInfoResponse getMyInfo(Long userId) {
        return toResponse(findUser(userId));
    }

    // 목표 변경
    @Transactional
    public MyInfoResponse updateMyInfo(Long userId, UpdateMyInfoRequest request) {
        User user = findUser(userId);

        // 없는 목표 id → 400
        Goal goal = goalRepository.findById(request.goalId())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));

        // 목표를 바꿔도 단계 체크 기록은 지우지 않음 → 다시 고르면 이어서
        user.changeGoal(goal);

        return toResponse(user);
    }

    // 조각 수 · 찾은 칸 수는 조각 기록에서 센다
    private MyInfoResponse toResponse(User user) {
        return MyInfoResponse.from(user,
                pieceAcquisitionRepository.countByUserId(user.getId()),
                pieceAcquisitionRepository.countFoundSlots(user.getId()));
    }

    private User findUser(Long userId) {
        // JWT 필터에서 이미 확인하지만, 그 사이 탈퇴한 경우 대비
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
    }
}