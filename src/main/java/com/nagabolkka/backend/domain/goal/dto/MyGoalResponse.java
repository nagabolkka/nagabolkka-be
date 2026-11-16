package com.nagabolkka.backend.domain.goal.dto;

import com.nagabolkka.backend.domain.goal.entity.Goal;
import com.nagabolkka.backend.domain.goal.entity.GoalStep;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Set;

public record MyGoalResponse(
        @Schema(example = "1") Long goalId,
        @Schema(example = "취업") String name,
        List<StepItem> steps,
        @Schema(description = "체크한 단계 수", example = "2") int checked,
        @Schema(description = "전체 단계 수", example = "4") int total,
        @Schema(description = "이 목표로 희망 조각을 받았는지", example = "false") boolean hopePieceReceived
) {

    public record StepItem(
            @Schema(example = "1") Long stepId,
            @Schema(example = "1") int stepNo,
            @Schema(example = "이력서 양식 하나 내려받기") String content,
            @Schema(example = "true") boolean checked
    ) {
    }

    public static MyGoalResponse of(Goal goal, List<GoalStep> steps, Set<Long> checkedIds, boolean hopePieceReceived) {
        List<StepItem> items = steps.stream()
                .map(step -> new StepItem(step.getId(), step.getStepNo(), step.getContent(),
                        checkedIds.contains(step.getId())))
                .toList();
        int checked = (int) items.stream().filter(StepItem::checked).count();
        return new MyGoalResponse(goal.getId(), goal.getName(), items, checked, items.size(), hopePieceReceived);
    }
}