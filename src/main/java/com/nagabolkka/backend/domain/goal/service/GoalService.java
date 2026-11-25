package com.nagabolkka.backend.domain.goal.service;

import com.nagabolkka.backend.domain.goal.dto.CheckGoalStepResponse;
import com.nagabolkka.backend.domain.goal.dto.CheckGoalStepResponse.HopePiece;
import com.nagabolkka.backend.domain.goal.dto.MyGoalResponse;
import com.nagabolkka.backend.domain.goal.entity.Goal;
import com.nagabolkka.backend.domain.goal.entity.GoalStep;
import com.nagabolkka.backend.domain.goal.entity.UserGoalStep;
import com.nagabolkka.backend.domain.goal.repository.GoalStepRepository;
import com.nagabolkka.backend.domain.goal.repository.UserGoalStepRepository;
import com.nagabolkka.backend.domain.piece.entity.AcquireSource;
import com.nagabolkka.backend.domain.piece.entity.Piece;
import com.nagabolkka.backend.domain.piece.entity.PieceAcquisition;
import com.nagabolkka.backend.domain.piece.entity.PieceType;
import com.nagabolkka.backend.domain.piece.repository.PieceAcquisitionRepository;
import com.nagabolkka.backend.domain.piece.repository.PieceRepository;
import com.nagabolkka.backend.domain.user.entity.User;
import com.nagabolkka.backend.domain.user.repository.UserRepository;
import com.nagabolkka.backend.global.exception.CustomException;
import com.nagabolkka.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoalService {

    private final UserRepository userRepository;
    private final GoalStepRepository goalStepRepository;
    private final UserGoalStepRepository userGoalStepRepository;
    private final PieceRepository pieceRepository;
    private final PieceAcquisitionRepository pieceAcquisitionRepository;

    // 내 목표 : 4단계 + 체크 상태 + 이 목표로 희망 조각을 받았는지
    public MyGoalResponse getMyGoal(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        Goal goal = currentGoal(user);

        List<GoalStep> steps = goalStepRepository.findByGoalOrderByStepNoAsc(goal);
        Set<Long> checkedIds = new HashSet<>(userGoalStepRepository.findCheckedStepIds(userId, goal.getId()));
        boolean hopePieceReceived = pieceAcquisitionRepository
                .existsByUserIdAndSourceAndGoalId(userId, AcquireSource.GOAL, goal.getId());

        return MyGoalResponse.of(goal, steps, checkedIds, hopePieceReceived);
    }

    // 단계 체크 / 해제 → 이 목표를 처음 4/4 완성하면 희망 조각
    @Transactional
    public CheckGoalStepResponse checkStep(Long userId, Long stepId, boolean checked) {
        // 연속 클릭 · 두 탭 동시 요청에도 체크 행 1개 · 희망 조각 1번 → 사용자 행 잠금
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        Goal goal = currentGoal(user);

        // 현재 목표의 단계가 아니면 404 (없는 id · 다른 목표 단계)
        GoalStep step = goalStepRepository.findById(stepId)
                .filter(s -> Objects.equals(s.getGoal().getId(), goal.getId()))
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        // 이미 같은 상태면 아무것도 안 바꿈 (두 번 눌러도 같은 결과)
        Set<Long> checkedIds = new HashSet<>(userGoalStepRepository.findCheckedStepIds(userId, goal.getId()));
        if (checked && checkedIds.add(stepId)) {
            userGoalStepRepository.save(UserGoalStep.of(user, step));
        } else if (!checked && checkedIds.remove(stepId)) {
            userGoalStepRepository.deleteByUserIdAndGoalStepId(userId, stepId);
        }

        int total = (int) goalStepRepository.countByGoalId(goal.getId());
        int checkedCount = checkedIds.size();
        HopePiece hopePiece = (total > 0 && checkedCount == total) ? grantHopePiece(user, goal) : null;

        return new CheckGoalStepResponse(checkedCount, total, hopePiece);
    }

    // 이 목표로 처음 4/4 → 희망 조각. 이미 받았으면 null (해제해도 회수 안 함, 복원도 미반영)
    private HopePiece grantHopePiece(User user, Goal goal) {
        if (pieceAcquisitionRepository.existsByUserIdAndSourceAndGoalId(user.getId(), AcquireSource.GOAL, goal.getId())) {
            return null;
        }
        Piece piece = pieceRepository.findFirstByType(PieceType.GOAL)
                .orElseThrow(() -> new IllegalStateException("희망 조각 시드가 없어요")); // 데이터 오류 → 500

        // 조각은 모든 목표 공통 1개 → 다른 목표로 이미 받았으면 NEW 아님
        boolean isNew = !pieceAcquisitionRepository.existsByUserIdAndPieceId(user.getId(), piece.getId());
        pieceAcquisitionRepository.save(PieceAcquisition.ofGoal(user, piece, goal));
        return new HopePiece(piece.getId(), piece.getName(), isNew);
    }

    private Goal currentGoal(User user) {
        if (!user.isOnboardingCompleted()) {
            throw new CustomException(ErrorCode.ONBOARDING_REQUIRED);
        }
        // 목표 없이 시작한 사용자 (시작 체크에서 목표 선택이 빠짐) → 404, 프론트는 「아직 비어 있어요」 빈 상태
        Goal goal = user.getCurrentGoal();
        if (goal == null) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
        return goal;
    }
}